//WZ目录入口

package net.sf.odinms.provider.wz;

import java.util.*;
import net.sf.odinms.provider.MapleDataDirectoryEntry;
import net.sf.odinms.provider.MapleDataEntity;
import net.sf.odinms.provider.MapleDataEntry;
import net.sf.odinms.provider.MapleDataFileEntry;


public class WZDirectoryEntry extends WZEntry implements MapleDataDirectoryEntry {

    private List<MapleDataDirectoryEntry> subdirs = new ArrayList<MapleDataDirectoryEntry>();
    private List<MapleDataFileEntry> files = new ArrayList<MapleDataFileEntry>();
    private Map<String, MapleDataEntry> entries = new HashMap<String, MapleDataEntry>();
    org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WZDirectoryEntry.class);

    public WZDirectoryEntry(String name, int size, int checksum, MapleDataEntity parent) {
        super(name, size, checksum, parent);
    }

    public WZDirectoryEntry() {
        super(null, 0, 0, null);
    }

    public void addDirectory(MapleDataDirectoryEntry dir) {
        subdirs.add(dir);
        entries.put(dir.getName(), dir);
    }

    public void addFile(MapleDataFileEntry fileEntry) {
        files.add(fileEntry);
        entries.put(fileEntry.getName(), fileEntry);
    }

    public List<MapleDataDirectoryEntry> getSubdirectories() {
        return Collections.unmodifiableList(subdirs);
    }

    public List<MapleDataFileEntry> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public MapleDataEntry getEntry(String name) {
        return entries.get(name);
    }
}