package com.dotcms.enterprise.publishing.bundlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class FileAssetBundlerTest {
    
    protected static User user=null;
    protected static Host host=null;
    
    @BeforeClass
    public static void init() throws Exception {
        user = APILocator.getUserAPI().getSystemUser();
        
        host = new Host();
        host.setHostname("FABT_"+UUIDGenerator.generateUuid());
        host.setLanguageId(1);
        host = APILocator.getHostAPI().save(host, user, false);
        APILocator.getContentletAPI().isInodeIndexed(host.getInode());
        
        Folder ffa = APILocator.getFolderAPI().createFolders("/a", host, user, false);
        Folder ffb = APILocator.getFolderAPI().createFolders("/a/b", host, user, false);
        Folder ffc = APILocator.getFolderAPI().createFolders("/a/b/c", host, user, false);
        
        createTextFile(ffa,"file1.txt","ffa file1");
        createTextFile(ffa,"file2.txt","ffa file2");
        createTextFile(ffa,"file3.txt","ffa file3");
        
        createTextFile(ffb,"file1.txt","ffb file1");
        createTextFile(ffb,"file2.txt","ffb file2");
        createTextFile(ffb,"file3.txt","ffb file3");
        
        createTextFile(ffc,"file1.txt","ffc file1");
        createTextFile(ffc,"file2.txt","ffc file2");
        createTextFile(ffc,"file3.txt","ffc file3");
    }
    
    protected static Contentlet createTextFile(Folder folder, String name, String content) throws Exception {
        Contentlet cont=new Contentlet();
        cont.setStructureInode(StructureCache.getStructureByVelocityVarName("fileAsset").getInode());
        cont.setLanguageId(1);
        cont.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, name);
        cont.setStringProperty(FileAssetAPI.TITLE_FIELD, name);
        cont.setFolder(folder.getInode());
        cont.setHost(folder.getHostId());
        
        File file=File.createTempFile("aaa", ".txt");
        FileUtils.writeStringToFile(file, content);
        cont.setBinary(FileAssetAPI.BINARY_FIELD, file);
        
        cont = APILocator.getContentletAPI().checkin(cont, user, false);
        APILocator.getVersionableAPI().setLive(cont);
        APILocator.getContentletIndexAPI().addContentToIndex(cont);
        
        APILocator.getContentletAPI().isInodeIndexed(cont.getInode(),true);
        
        return cont;
    }

    public static class DummyPublisher extends Publisher {
        public DummyPublisher() {}
        
        public PublisherConfig process(PublishStatus status) throws DotPublishingException {
            return null;
        }
        public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
            return config;
        }
        public List<Class> getBundlers() {
            ArrayList<Class> bundlers=new ArrayList<Class>();
            bundlers.add(FileAssetBundler.class);
            return bundlers;
        }
    }
    
    @Test
    public void bundleWholeHost() throws Exception {
        final String bundleId=UUIDGenerator.generateUuid();
        
        final PublisherConfig conf=new PublisherConfig();
        
        ArrayList<Host> hosts=new ArrayList<Host>();
        hosts.add(host);
        conf.setHosts(hosts);
        conf.setLanguage(1);
        conf.setUser(user);
        conf.setId(bundleId);
        
        
        ArrayList<Class> publishers = new ArrayList<Class>();
        publishers.add(DummyPublisher.class);
        conf.setPublishers(publishers);
        
        PublishStatus status = APILocator.getPublisherAPI().publish(conf);
        
        Assert.assertEquals(0, status.getBundleErrors());
        Assert.assertEquals(18,status.getTotalBundleWork()); // 9 working, 9 live
        
        File root = BundlerUtil.getBundleRoot(conf);
        File hostbase = new File(root,"live"+File.separator+host.getHostname()+File.separator+"1");
        Assert.assertTrue(hostbase.exists());
        
        // check folders
        
        File ffa=new File(hostbase,"a");
        Assert.assertTrue(ffa.exists());
        Assert.assertTrue(ffa.isDirectory());
        
        File ffb=new File(hostbase,"a"+File.separator+"b");
        Assert.assertTrue(ffb.exists());
        Assert.assertTrue(ffb.isDirectory());
        
        File ffc=new File(hostbase,"a"+File.separator+"b"+File.separator+"c");
        Assert.assertTrue(ffc.exists());
        Assert.assertTrue(ffc.isDirectory());
        
        XStream xtream = new XStream(new DomDriver());
        
        // check files
        
        File filea1=new File(ffa,"file1.txt");
        Assert.assertTrue(filea1.exists());
        Assert.assertEquals("ffa file1", FileUtils.readFileToString(filea1));
        
        FileAssetWrapper wrapper=(FileAssetWrapper) xtream.fromXML(new File(filea1.getAbsolutePath()+".fileAsset.xml"));
        Assert.assertEquals("file1.txt", wrapper.getAsset().getTitle());
        Assert.assertEquals("file1.txt", wrapper.getAsset().getFileName());
        Assert.assertEquals(host.getIdentifier(), wrapper.getAsset().getHost());
        Folder fffa = APILocator.getFolderAPI().findFolderByPath("/a", host, user, false);
        Assert.assertEquals(fffa.getInode(), wrapper.getAsset().getFolder());
        Assert.assertEquals(wrapper.getAsset().getInode(), wrapper.getInfo().getWorkingInode());
        Assert.assertEquals(wrapper.getAsset().getInode(), wrapper.getInfo().getLiveInode());
        
        File filea2=new File(ffa,"file2.txt");
        Assert.assertTrue(filea2.exists());
        Assert.assertEquals("ffa file2", FileUtils.readFileToString(filea2));
        
        File filea3=new File(ffa,"file3.txt");
        Assert.assertTrue(filea3.exists());
        Assert.assertEquals("ffa file3", FileUtils.readFileToString(filea3));
        
        File fileb1=new File(ffb,"file1.txt");
        Assert.assertTrue(fileb1.exists());
        Assert.assertEquals("ffb file1", FileUtils.readFileToString(fileb1));
        
        File fileb2=new File(ffb,"file2.txt");
        Assert.assertTrue(fileb2.exists());
        Assert.assertEquals("ffb file2", FileUtils.readFileToString(fileb2));
 
        File fileb3=new File(ffb,"file3.txt");
        Assert.assertTrue(fileb3.exists());
        Assert.assertEquals("ffb file3", FileUtils.readFileToString(fileb3));
        
        File filec1=new File(ffc,"file1.txt");
        Assert.assertTrue(filec1.exists());
        Assert.assertEquals("ffc file1", FileUtils.readFileToString(filec1));
        
        File filec2=new File(ffc,"file2.txt");
        Assert.assertTrue(filec2.exists());
        Assert.assertEquals("ffc file2", FileUtils.readFileToString(filec2));
        
        File filec3=new File(ffc,"file3.txt");
        Assert.assertTrue(filec3.exists());
        Assert.assertEquals("ffc file3", FileUtils.readFileToString(filec3));
        
    }
    
}
