package com.dotmarketing.servlets;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Trackback;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TrackbackFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Save the trackback pin request or Redirect the permanentlink href macros. If
 * the passed identifier belongs to an html page the servlet just have to
 * forward the page url. E.G. http://dotcms.org/permalink/123 -> 123 =
 * /home/products.dot -> forward to /home/products.dot If the passed identifier
 * belongs to a file the servlet just have to forward to the speedy assets
 * servlet (/dotAsset/{identifier}.{ext}) http://dotcms.org/permalink/123 -> 123
 * = /home/myfile.pdf -> forward to /dotAsset/123.pdf If the passed inode is a
 * contentlet then servlet should use the second parameter, the detail page
 * identifier to the determine where to forward and pass the given identifier
 * that page. E.G. http://dotcms.org/permalink/123/567 -> 123 = My News Release
 * Content, 567 = /home/news_detail_page.dot -> forward to
 * /home/news_detail_page.dot?id=123
 * 
 * @author Oswaldo Gallango
 * @version 1.0
 * @since 1.5
 */
public class PermalinkServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException {

	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if (Config.CONTEXT == null) {
			Logger.debug(this, "Link not Found");
			response.sendError(404, "Link not Found");
			return;
		}

		// if this is a blog trackback
		if (UtilMethods.isSet(request.getParameter("blog_name")) || UtilMethods.isSet(request.getParameter("excerpt"))
				|| UtilMethods.isSet(request.getParameter("title")) || UtilMethods.isSet(request.getParameter("url"))) {

			processTrackBackPing(request, response);
			return;
		}

		String serverName = request.getServerName();

		StringTokenizer urlString = new StringTokenizer(request.getRequestURI(), "/");
		urlString.nextToken();

		String id = urlString.nextToken();

		try {
			User user = WebAPILocator.getUserWebAPI().getLoggedInFrontendUser(request);
			Host host = WebAPILocator.getHostWebAPI().resolveHostName(serverName, user, true);
			Identifier identifier = null;
			try {
				identifier = APILocator.getIdentifierAPI().find(id);
			} catch (Exception e) {
				throw new Exception("No valid Identifier found");
			}
			if (identifier == null || !UtilMethods.isSet(identifier.getId())) {
				throw new Exception("No valid Identifier found");
			}

			if ("contentlet".equalsIgnoreCase(identifier.getAssetType())) {
				ContentletAPI capi = APILocator.getContentletAPI();
				Language l = WebAPILocator.getLanguageWebAPI().getLanguage(request);

				Contentlet c = (Contentlet) capi.findContentletByIdentifier(identifier.getId(), true, l.getId(), user, true);
				if (c == null || !UtilMethods.isSet(c.getInode())) {
					throw new Exception("No valid Contentlet found");
				}
				String x = capi.getUrlMapForContentlet(c, user, true);
				if ("URL_MAP_FOR_CONTENT_404".equals(x)) {
					throw new Exception("No valid urlmap found");
				}

				// if the content is on the system host (shared) or on the
				// current host
				if (c.getHost().equals(APILocator.getHostAPI().findSystemHost().getIdentifier())
						|| c.getHost().equals(host.getIdentifier())) {
					response.sendRedirect(x);
					return;
				}

			}

			if ("htmlpage".equalsIgnoreCase(identifier.getAssetType())) {
				if (host.getIdentifier().equals(identifier.getHostId())) {
					response.sendRedirect(identifier.getURI());
					return;

				}
			}

		} catch (Exception ex) {

			Logger.error(this, "PERMALink not Found id:" + id + " : " + ex);

		}
		response.sendError(404, "PermaLink not Found");

	}

	/**
	 * This method process the trackback ping saving in db the trackback if the
	 * info is correct and return a XML message indicating if the ping was
	 * successfull or the error
	 * 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void processTrackBackPing(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {

		StringBuffer message = new StringBuffer();
		ServletOutputStream out = response.getOutputStream();
		try {
			StringTokenizer urlString = new StringTokenizer(request.getRequestURI(), "/");
			urlString.nextToken();

			String _identifier = null;

			_identifier = urlString.nextToken();
			Trackback tb = new Trackback();
			tb.setAssetIdentifier(_identifier);
			if (UtilMethods.isSet(request.getParameter("blog_name"))) {
				tb.setBlogName(request.getParameter("blog_name"));
			}
			if (UtilMethods.isSet(request.getParameter("excerpt"))) {
				tb.setExcerpt(request.getParameter("excerpt"));
			}
			if (UtilMethods.isSet(request.getParameter("title"))) {
				tb.setTitle(request.getParameter("title"));
			}
			if (UtilMethods.isSet(request.getParameter("url"))) {
				tb.setUrl(request.getParameter("url"));
			} else {
				message.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
				message.append("<response><error>1</error><message>");
				message.append("url parameter is required");
				message.append("</message></response>");
				out.print(message.toString());
				return;
			}

			TrackbackFactory.save(tb);

			message.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			message.append("<response><error>0</error>");
			message.append("</response>");
			out.print(message.toString());

		} catch (Exception e) {

			message.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			message.append("<response><error>1</error><message>");
			message.append(URLEncoder.encode(e.getMessage(), "utf-8"));
			message.append("</message></response>");
			out.print(message.toString());

		} finally {
			out.close();
		}
	}

}
