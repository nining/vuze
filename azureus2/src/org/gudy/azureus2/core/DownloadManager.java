/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core2.PeerSocket;
import org.gudy.azureus2.ui.swt.IComponentListener;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier
 * 
 */
public class DownloadManager extends Component {

  private int state;
  public static final int STATE_WAITING = 0;
  public static final int STATE_INITIALIZING = 5;
  public static final int STATE_INITIALIZED = 10;
  public static final int STATE_ALLOCATING = 20;
  public static final int STATE_CHECKING = 30;
  public static final int STATE_READY = 40;
  public static final int STATE_DOWNLOADING = 50;
  public static final int STATE_SEEDING = 60;
  public static final int STATE_STOPPING = 65;
  public static final int STATE_STOPPED = 70;
  public static final int STATE_ERROR = 100;
  
  private int priority;
  public static final int LOW_PRIORITY = 1;
  public static final int HIGH_PRIORITY = 2;


  private String errorDetail;

  private GlobalManager globalManager;

  private String torrentFileName;
  private String name;
  private int nbPieces;
  private String savePath;

  private StringBuffer metaInfo;
  private byte[] hash;
  private Map metaData;
  private Server server;
  private TrackerConnection trackerConnection;
  public DiskManager diskManager;
  public PeerManager peerManager;

  private int maxUploads = 4;

  public DownloadManager(GlobalManager gm, String torrentFileName, String savePath,boolean stopped) {
   this(gm,torrentFileName,savePath);
   if (this.state == STATE_ERROR)
     return;
   if(stopped)
    this.state = STATE_STOPPED;
  }

  public DownloadManager(GlobalManager gm, String torrentFileName, String savePath) {
    this.globalManager = gm;
    this.maxUploads = ConfigurationManager.getInstance().getIntParameter("Max Uploads", 4); //$NON-NLS-1$
    this.state = STATE_WAITING;
    this.priority = HIGH_PRIORITY;
    this.torrentFileName = torrentFileName;
    this.savePath = savePath;
    this.metaInfo = new StringBuffer();
    extractMetaInfo();
    if (this.state == STATE_ERROR)
      return;
  }

  public void initialize() {
    this.state = STATE_INITIALIZING;
    startServer();
    if (this.state == STATE_ERROR)
      return;

    trackerConnection = new TrackerConnection(metaData, hash, server.getPort());
    this.state = STATE_INITIALIZED;
    diskManager = new DiskManager(metaData, savePath);
  }

  public void startDownload() {
    this.state = STATE_DOWNLOADING;
    peerManager = new PeerManager(this, hash, server, trackerConnection, diskManager);
  }

  private void extractMetaInfo() {

    try {
      byte[] buf = new byte[1024];
      int nbRead;
      FileInputStream fis = new FileInputStream(torrentFileName);
      while ((nbRead = fis.read(buf)) > 0)
        metaInfo.append(new String(buf, 0, nbRead, "ISO-8859-1")); //$NON-NLS-1$
      fis.close();
      metaData = BDecoder.decode(metaInfo.toString().getBytes("ISO-8859-1")); //$NON-NLS-1$
      Map info = (Map) metaData.get("info"); //$NON-NLS-1$
      name = new String((byte[]) info.get("name"), "ISO-8859-1"); //$NON-NLS-1$ //$NON-NLS-2$
      byte[] pieces = (byte[]) info.get("pieces"); //$NON-NLS-1$
      nbPieces = pieces.length / 20;
      metaData.put("torrent filename", torrentFileName.getBytes("ISO-8859-1")); //$NON-NLS-1$ //$NON-NLS-2$
      SHA1Hasher s = new SHA1Hasher();
      hash = s.calculateHash(BEncoder.encode((Map) metaData.get("info"))); //$NON-NLS-1$
    }
    catch (FileNotFoundException e) {
      name = Messages.getString("DownloadManager.error.filenotfound"); //$NON-NLS-1$
      nbPieces = 0;
      hash = new byte[20];      
      this.state = STATE_ERROR;
      errorDetail = Messages.getString("DownloadManager.error.filenotfound"); //$NON-NLS-1$
    }
    catch (UnsupportedEncodingException e) {
      this.state = STATE_ERROR;
      errorDetail = Messages.getString("DownloadManager.error.unsupportedencoding"); //$NON-NLS-1$
    }
    catch (IOException e) {
      this.state = STATE_ERROR;
      errorDetail = Messages.getString("DownloadManager.error.ioerror"); //$NON-NLS-1$
    }
    catch (NoSuchAlgorithmException e) {
      this.state = STATE_ERROR;
      errorDetail = Messages.getString("DownloadManager.error.sha1"); //$NON-NLS-1$
    }
    catch (Exception e) {
      this.state = STATE_ERROR;
      errorDetail = e.getMessage();
    }
  }

  private void startServer() {
    server = new Server();
    int port = server.getPort();
    if (port == 0) {
      this.state = STATE_ERROR;
      errorDetail = Messages.getString("DownloadManager.error.unabletostartserver"); //$NON-NLS-1$
    }
  }

  /**
   * @return
   */
  public int getState() {
    if (state != STATE_INITIALIZED)
      return state;
    if (diskManager == null)
      return STATE_INITIALIZED;
    int diskManagerState = diskManager.getState();
    if (diskManagerState == DiskManager.INITIALIZING)
      return STATE_INITIALIZED;
    if (diskManagerState == DiskManager.ALLOCATING)
      return STATE_ALLOCATING;
    if (diskManagerState == DiskManager.CHECKING)
      return STATE_CHECKING;
    if (diskManagerState == DiskManager.READY)
      return STATE_READY;
    return STATE_ERROR;
  }

  public String getName() {
    if (diskManager == null)
      return name;
    return diskManager.getFileName();
  }

  public int getCompleted() {
    if (diskManager == null)
      return 0;
    if (diskManager.getState() == DiskManager.ALLOCATING
      || diskManager.getState() == DiskManager.CHECKING
      || diskManager.getState() == DiskManager.INITIALIZING)
      return diskManager.getPercentDone();
    else {
      long remaining = diskManager.getRemaining();
      long total = diskManager.getTotalLength();
      return (int) ((1000 * (total - remaining)) / total);
    }
  }

  public String getErrorDetails() {
    return errorDetail;
  }

  public long getSize() {
    if (diskManager == null)
      return 0;
    return diskManager.getTotalLength();
  }

  public boolean[] getPiecesStatus() {
    if (peerManager != null)
      return peerManager.getPiecesStatus();
    if (diskManager != null)
      return diskManager.getPiecesStatus();
    return new boolean[nbPieces];
  }

  public void stopIt() {
    Thread stopThread = new Thread() {
      public void run() {
        state = DownloadManager.STATE_STOPPING;
        if (peerManager != null)
          peerManager.stopAll();
        peerManager = null;
        if (diskManager != null)
          diskManager.stopIt();
        trackerConnection = null;
        state = DownloadManager.STATE_STOPPED;
      }
    };
    stopThread.start();
  }

  public void setState(int state) {
    this.state = state;
  }

  public int getNbSeeds() {
    if (peerManager != null)
      return peerManager.getNbSeeds();
    return 0;
  }

  public int getNbPeers() {
    if (peerManager != null)
      return peerManager.getNbPeers();
    return 0;
  }

  public String getDownloadSpeed() {
    if (peerManager != null)
      return peerManager.getStats().getReceptionSpeed();
    return ""; //$NON-NLS-1$
  }

  public String getUploadSpeed() {
    if (peerManager != null)
      return peerManager.getStats().getSendingSpeed();
    return ""; //$NON-NLS-1$
  }

  public String getTrackerStatus() {
    if (peerManager != null)
      return peerManager.getTrackerStatus();
    return ""; //$NON-NLS-1$
  }

  public String getETA() {
    if (peerManager != null)
      return peerManager.getETA();
    return ""; //$NON-NLS-1$
  }

  /**
   * @return
   */
  public int getMaxUploads() {
    return maxUploads;
  }

  /**
   * @param i
   */
  public void setMaxUploads(int i) {
    maxUploads = i;
  }

  /**
   * @return
   */
  public int getNbPieces() {
    return nbPieces;
  }

  public String getElapsed() {
    if (peerManager != null)
      return peerManager.getElpased();
    return ""; //$NON-NLS-1$
  }

  public String getDownloaded() {
    if (peerManager != null)
      return peerManager.getDownloaded();
    return ""; //$NON-NLS-1$
  }

  public String getUploaded() {
    if (peerManager != null)
      return peerManager.getUploaded();
    return ""; //$NON-NLS-1$
  }

  public String getTotalSpeed() {
    if (peerManager != null)
      return peerManager.getTotalSpeed();
    return ""; //$NON-NLS-1$
  }

  public int getTrackerTime() {
    if (peerManager != null)
      return peerManager.getTrackerTime();
    return 60;
  }

  /**
   * @return
   */
  public byte[] getHash() {
    return hash;
  }

  /**
   * @return
   */
  public String getSavePath() {
    return savePath;
  }

  public String getPieceLength() {
    if (diskManager != null)
      return PeerStats.format(diskManager.getPieceLength());
    return ""; //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponent#addListener(org.gudy.azureus2.ui.swt.IComponentListener)
   */
  public void addListener(IComponentListener listener) {
    // TODO Auto-generated method stub
    super.addListener(listener);
    if (peerManager != null) {
      List connections = peerManager.get_connections();
      synchronized (connections) {
        for (int i = 0; i < connections.size(); i++) {
          PeerSocket ps = (PeerSocket) connections.get(i);
          if (ps.getState() == PeerSocket.TRANSFERING)
            objectAdded(ps);
        }
      }
      Piece[] pieces = peerManager.getPieces();
      for (int i = 0; i < pieces.length; i++) {
        if (pieces[i] != null) {
          objectAdded(pieces[i]);
        }
      }
    }
  }

  public String getFileName() {
    if (diskManager != null)
      return savePath + System.getProperty("file.separator") + diskManager.getFileName(); //$NON-NLS-1$
    return savePath;
  }

  public void received(int length) {
    if (length > 0) {
      globalManager.received(length);
    }
  }

  public void sent(int length) {
    if (length > 0)
      globalManager.sent(length);
  }

  /**
   * @return
   */
  public int getPriority() {
    return priority;
  }

  /**
   * @param i
   */
  public void setPriority(int i) {
    priority = i;
  }

  /**
   * @return
   */
  public String getTorrentFileName() {
    return torrentFileName;
  }

  /**
   * @param string
   */
  public void setTorrentFileName(String string) {
    torrentFileName = string;
  }

}
