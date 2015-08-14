package net.sf.odinms.net.channel;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.*;
import net.sf.odinms.client.MapleCharacter;

public class PlayerStorage implements IPlayerStorage {

	Map<String, MapleCharacter> nameToChar = new ConcurrentHashMap<String, MapleCharacter>();
	Map<Integer, MapleCharacter> idToChar = new ConcurrentHashMap<Integer, MapleCharacter>();
	Lock lock = new ReentrantLock(true);

	public void registerPlayer(MapleCharacter chr) {
		try {
			lock.lock();
			nameToChar.put(chr.getName().toLowerCase(), chr);
			idToChar.put(chr.getId(), chr);
		} finally {
			lock.unlock();
		}
	}

	public void deregisterPlayer(MapleCharacter chr) {
		try {
			lock.lock();
			nameToChar.remove(chr.getName().toLowerCase());
			idToChar.remove(chr.getId());
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean hasCharacter(int id) {
		return idToChar.containsKey(id);
	}

	@Override
	public boolean hasCharacter(String name) {
		return nameToChar.containsKey(name);
	}

	@Override
	public synchronized MapleCharacter getCharacterByName(String name) {
		return nameToChar.get(name.toLowerCase());
	}

	@Override
	public synchronized MapleCharacter getCharacterById(int id) {
		return idToChar.get(Integer.valueOf(id));
	}

	@Override
	public synchronized Collection<MapleCharacter> getAllCharacters() {
		return nameToChar.values();
	}
}