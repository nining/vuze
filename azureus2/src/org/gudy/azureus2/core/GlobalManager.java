/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.ui.swt.IComponentListener;

/**
 * @author Olivier
 * 
 */
public class GlobalManager extends Component {

  private List managers;
  private Checker checker;
  private PeerStats stats;
  private TrackerChecker trackerChecker;

  public class Checker extends Thread {
    boolean finished = false;
    int loopFactor;
    private static final int waitTime = 1000;
		// 5 minutes save resume data interval (default)
    private int saveResumeLoopCount = 300000 / waitTime;

    public Checker() {
      super("Global Status Checker");
      loopFactor = 0;
      setPriority(Thread.MIN_PRIORITY);
      //determineSaveResumeDataInterval();
    }

    private void determineSaveResumeDataInterval() {
      int saveResumeInterval = ConfigurationManager.getInstance().getIntParameter("Save Resume Interval", 5);
      if(saveResumeInterval > 1 && saveResumeInterval < 21)
        saveResumeLoopCount = saveResumeInterval * 60000 / waitTime;
    }

    public void run() {
      while (!finished) {

        loopFactor++;
        determineSaveResumeDataInterval();
        // Changed to 20 mins :D
        // Should be user configurable.
        if (loopFactor >= 1200) {
          loopFactor = 0;          
          trackerChecker.update();
        }

        synchronized (managers) {
          int nbStarted = 0;
          int nbDownloading = 0;
          if (loopFactor % saveResumeLoopCount == 0) {
             saveDownloads();
          }
                                  
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if (manager.getState() == DownloadManager.STATE_DOWNLOADING) {
              nbStarted++;
              nbDownloading++;
              if (loopFactor % saveResumeLoopCount == 0) {
                manager.diskManager.dumpResumeDataToDisk(false);
              }
            }
            else if (manager.getState() == DownloadManager.STATE_SEEDING) {
              nbStarted++;
              //Checks if any condition to stop seeding is met
              int minShareRatio = 1000 * ConfigurationManager.getInstance().getIntParameter("Stop Ratio",0);
              //0 means unlimited
              if(minShareRatio != 0) {
                int shareRatio = manager.getShareRatio();
                if(shareRatio > minShareRatio)
                  manager.stopIt();
              }
              
              
              int minSeedsPerPeersRatio = ConfigurationManager.getInstance().getIntParameter("Stop Peers Ratio",0);    
              //0 means never stop
              if(minSeedsPerPeersRatio != 0) {
                HashData hd = manager.getHashData();    
                if(hd != null) {            
                  int nbPeers = hd.peers;
                  int nbSeeds = hd.seeds;
                  //If there are no seeds, avoid / by 0
                  if(nbSeeds != 0) {
                    int ratio = nbPeers / nbSeeds;
                    if(ratio < minSeedsPerPeersRatio)
                      manager.stopIt();
                  }
                }
              }         
            } else if(manager.getState() == DownloadManager.STATE_STOPPED && manager.getCompleted() == 1000) {
              //Checks if any condition to start seeding is met
              int minSeedsPerPeersRatio = ConfigurationManager.getInstance().getIntParameter("Start Peers Ratio",0); 
              //0 means never start
              if(minSeedsPerPeersRatio != 0) {
                HashData hd = manager.getHashData();  
                if(hd != null) {              
                  int nbPeers = hd.peers;
                  int nbSeeds = hd.seeds;
                  //If there are no seeds, avoid / by 0
                  if(nbPeers != 0) {
                    if(nbSeeds != 0) {                  
                      int ratio = nbPeers / nbSeeds;
                      if(ratio >= minSeedsPerPeersRatio)
                        manager.setState(DownloadManager.STATE_WAITING);
                    } else {
                      //No seeds, at least 1 peer, let's start download.
                      manager.setState(DownloadManager.STATE_WAITING);
                    }
                  }
                }
              }
            }
          }
          boolean alreadyOneAllocatingOrChecking = false;
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if (((manager.getState() == DownloadManager.STATE_ALLOCATING)
              || (manager.getState() == DownloadManager.STATE_CHECKING)
              || (manager.getState() == DownloadManager.STATE_INITIALIZED))) {
              alreadyOneAllocatingOrChecking = true;
            }
          }
          
          for (int i = 0; i < managers.size(); i++) {
            DownloadManager manager = (DownloadManager) managers.get(i);
            if ((manager.getState() == DownloadManager.STATE_WAITING) && !alreadyOneAllocatingOrChecking) {
              manager.initialize();
              alreadyOneAllocatingOrChecking = true;
            }
            int nbMax = ConfigurationManager.getInstance().getIntParameter("max active torrents", 4);
            int nbMaxDownloads = ConfigurationManager.getInstance().getIntParameter("max downloads", 4);
            if (manager.getState() == DownloadManager.STATE_READY
              && ((nbMax == 0) || (nbStarted < nbMax))
              && (manager.getCompleted() == 1000 ||  ((nbMaxDownloads == 0) || (nbDownloading < nbMaxDownloads)))) {
              manager.startDownload();
              nbStarted++;
              if (manager.getCompleted() != 1000)
                nbDownloading++;
            }

            if (manager.getState() == DownloadManager.STATE_ERROR) {
              DiskManager dm = manager.diskManager;
              if (dm != null && dm.getState() == DiskManager.FAULTY)
                manager.setErrorDetail(dm.getErrorMessage());
            }

            if ((manager.getState() == DownloadManager.STATE_SEEDING)
              && (manager.getPriority() == DownloadManager.HIGH_PRIORITY)
              && ConfigurationManager.getInstance().getBooleanParameter("Switch Priority", true)) {
              manager.setPriority(DownloadManager.LOW_PRIORITY);
            }

            if ((manager.getState() == DownloadManager.STATE_ERROR)
              && (manager.getErrorDetails() != null && manager.getErrorDetails().equals("File Not Found"))) {
              removeDownloadManager(manager);
            }
          }
        }
        try {
          Thread.sleep(waitTime);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void stopIt() {
      finished = true;
    }
  }

  public GlobalManager() {
    stats = new PeerStats(0);
    managers = new ArrayList();
    trackerChecker = new TrackerChecker();
    loadDownloads();
    checker = new Checker();
    checker.start();
  }

  public boolean addDownloadManager(String fileName, String savePath) {
    DownloadManager manager = new DownloadManager(this, fileName, savePath);
    return addDownloadManager(manager);
  }


  //Public method !!! and don't touch it !
  public boolean addDownloadManager(DownloadManager manager) {
    synchronized (managers) {
      if (managers.contains(manager)) {
        manager.setState(DownloadManager.STATE_DUPLICATE);
        return false;
      }
      managers.add(manager);
    }

    objectAdded(manager);
    saveDownloads();
    return true;
  }

  public List getDownloadManagers() {
    return managers;
  }

  public void removeDownloadManager(DownloadManager manager) {
    synchronized (managers) {
      managers.remove(manager);
    }
    this.objectRemoved(manager);
    saveDownloads();
    this.trackerChecker.removeHash(manager.getTrackerUrl(),new Hash(manager.getHash()));
  }

  public void stopAll() {
    checker.stopIt();
    saveDownloads();
    while (managers.size() != 0) {
      DownloadManager manager = (DownloadManager) managers.remove(0);
      manager.stopIt();
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponent#addListener(org.gudy.azureus2.ui.swt.IComponentListener)
   */
  public void addListener(IComponentListener listener) {
    // TODO Auto-generated method stub
    super.addListener(listener);
    synchronized (managers) {
      for (int i = 0; i < managers.size(); i++) {
        listener.objectAdded(managers.get(i));
      }
    }
  }

  public void received(int length) {
    stats.received(length);
  }
  
  public void discarded(int length) {
      stats.discarded(length);
    }

  public void sent(int length) {
    stats.sent(length);
  }

  public String getDownloadSpeed() {
    return stats.getReceptionSpeed();
  }

  public String getUploadSpeed() {
    return stats.getSendingSpeed();
  }

  private void loadDownloads() {
    FileInputStream fin = null;
    BufferedInputStream bin = null;
    try {
      //open the file
      File configFile = getApplicationFile("downloads.config");
      fin = new FileInputStream(configFile);
      bin = new BufferedInputStream(fin);
      Map map = BDecoder.decode(bin);
      boolean debug = Boolean.getBoolean("debug");
      
      Iterator iter = null;
      //v2.0.3.0+ vs older mode
      List downloads = (List) map.get("downloads");
      if(downloads == null) {
        //No downloads entry, then use the old way
        iter = map.values().iterator();
      } else {
        //New way, downloads stored in a list
        iter = downloads.iterator();
      }
      while (iter.hasNext()) {
        Map mDownload = (Map) iter.next();
        try {
          String fileName = new String((byte[]) mDownload.get("torrent"), Constants.DEFAULT_ENCODING);
          String savePath = new String((byte[]) mDownload.get("path"), Constants.DEFAULT_ENCODING);
          int nbUploads = ((Long) mDownload.get("uploads")).intValue();
          int stopped = debug ? 1 : ((Long) mDownload.get("stopped")).intValue();
          Long lPriority = (Long) mDownload.get("priority");   
          Long lDownloaded = (Long) mDownload.get("downloaded");
          Long lUploaded = (Long) mDownload.get("uploaded");
          Long lCompleted = (Long) mDownload.get("completed");
          DownloadManager dm = new DownloadManager(this, fileName, savePath, stopped == 1);
          dm.setMaxUploads(nbUploads);
          if(lPriority != null) {
            dm.setPriority(lPriority.intValue());
          }
          if(lDownloaded !=  null && lUploaded != null) {
            dm.setDownloadedUploaded(lDownloaded.longValue(),lUploaded.longValue());
          }
          if(lCompleted !=  null ) {
            dm.setCompleted(lCompleted.intValue());
          }
          this.addDownloadManager(dm);
        }
        catch (UnsupportedEncodingException e1) {
          //Do nothing and process next.
        }
      }
    }
    catch (FileNotFoundException e) {
      //Do nothing
    }
    catch (Exception e) {
      // TODO Auto-generated catch block     
    }
    finally {
      try {
        if (bin != null)
          bin.close();
      }
      catch (Exception e) {}
      try {
        if (fin != null)
          fin.close();
      }
      catch (Exception e) {}
    }
  }

  private void saveDownloads() {
    //    if(Boolean.getBoolean("debug")) return;

    Map map = new HashMap();
    List list = new ArrayList(managers.size());
    for (int i = 0; i < managers.size(); i++) {
      DownloadManager dm = (DownloadManager) managers.get(i);
      Map dmMap = new HashMap();
      dmMap.put("torrent", dm.getTorrentFileName());
      dmMap.put("path", dm.getSavePathForSave());
      dmMap.put("uploads", new Long(dm.getMaxUploads()));
      int stopped = 0;
      if (dm.getState() == DownloadManager.STATE_STOPPED)
        stopped = 1;
      dmMap.put("stopped", new Long(stopped));
      int priority = dm.getPriority();
      dmMap.put("priority", new Long(priority));
      dmMap.put("position", new Long(i));
      dmMap.put("downloaded",new Long(dm.getDownloadedRaw()));
      dmMap.put("uploaded",new Long(dm.getUploadedRaw()));
      dmMap.put("completed",new Long(dm.getCompleted()));
      list.add(dmMap);
    }
    map.put("downloads",list);
    //encode the data
    byte[] torrentData = BEncoder.encode(map);
    //open a file stream
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(getApplicationFile("downloads.config"));
      //write the data out
      fos.write(torrentData);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (fos != null)
          fos.close();
      }
      catch (Exception e) {}
    }
  }

  //TODO:: Move this to a FileManager class?
  public static String getApplicationPath() {
    return System.getProperty("user.dir");
  }

  public static File getApplicationFile(String filename) {
    return new File(getApplicationPath(), filename);
  }

  /**
   * @return
   */
  public TrackerChecker getTrackerChecker() {
    return trackerChecker;
  }
  
  public int getIndexOf(DownloadManager manager) {
    if(managers != null && manager != null)
      return managers.indexOf(manager);
    return -1;
  }
  
  public boolean isMoveableUp(DownloadManager manager) {
    return getIndexOf(manager) > 0;
  }
  
  public boolean isMoveableDown(DownloadManager manager) {
    if(managers != null)
      return getIndexOf(manager) < managers.size() -1;
    return false;
  }
  
  public void moveUp(DownloadManager manager) {    
    if(managers != null) {
      synchronized(managers) {
        int index = managers.indexOf(manager);
        if(index > 0) {
          managers.remove(index);
          managers.add(index-1,manager);
        }
      }
    }
  }
  
  public void moveDown(DownloadManager manager) {
    if(managers != null) {
      synchronized(managers) {
        int index = managers.indexOf(manager);
        if(index < managers.size() -1) {
          managers.remove(index);
          managers.add(index+1,manager);
        }
      }
    }
  }

}
