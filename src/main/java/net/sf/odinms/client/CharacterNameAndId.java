
package net.sf.odinms.client;

public class CharacterNameAndId {

    private int id;
    private String name;
    private int level;

    public CharacterNameAndId(int id, String name, int level) {
        super();
        this.id = id;
        this.name = name;
        this.level = level;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public int getLevel() {
        return level;
    }

}