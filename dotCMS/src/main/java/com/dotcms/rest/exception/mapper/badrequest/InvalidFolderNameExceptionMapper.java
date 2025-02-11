package com.dotcms.rest.exception.mapper.badrequest;

import com.dotcms.rest.exception.mapper.DotBadRequestExceptionMapper;
import com.dotmarketing.portlets.folders.exception.InvalidFolderNameException;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidFolderNameExceptionMapper
        extends DotBadRequestExceptionMapper<InvalidFolderNameException> {

}