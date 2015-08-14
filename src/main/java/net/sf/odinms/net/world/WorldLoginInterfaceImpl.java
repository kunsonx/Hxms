package net.sf.odinms.net.world;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Properties;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import net.sf.odinms.net.channel.ChannelDescriptor;
import net.sf.odinms.net.login.remote.ChannelLoadInfo;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.remote.WorldLoginInterface;

/**
 * Login 对 World 的操作实例类 [run at WorldServer]
 *
 * @author Matze
 */
public class WorldLoginInterfaceImpl extends UnicastRemoteObject implements WorldLoginInterface {

    private static final long serialVersionUID = -4965323089596332908L;

    public WorldLoginInterfaceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

 
    @Override
    public boolean isAvailable() throws RemoteException {
        return true;
    }

    @Override
    public ChannelLoadInfo getChannelLoad() throws RemoteException {
        return new ChannelLoadInfo(WorldRegistryImpl.getInstance().getChannelServerStorage());
    }

    @Override
    public void deleteGuildCharacter(MapleGuildCharacter mgc) throws RemoteException {
        WorldRegistryImpl wr = WorldRegistryImpl.getInstance();

        wr.setGuildMemberOnline(mgc, false, -1);

        if (mgc.getGuildRank() > 1) //not leader
        {
            wr.leaveGuild(mgc);
        } else {
            wr.disbandGuild(mgc.getGuildId());
        }
    }

    @Override
    public long checkClientIvKey(byte[] ivCheck, String port) throws RemoteException {
        ChannelDescriptor cd = null;
     /*   for (Map.Entry<ChannelDescriptor, String> entry : WorldRegistryImpl.getInstance().getChannelServerStorage().getPorts().entrySet()) {
            if (entry.getValue().equals(port)) {
                cd = entry.getKey();
                break;
            }
        }*/
        if (cd != null) {
            return WorldRegistryImpl.getInstance().getChannel(cd).checkClientIvKey(ivCheck);
        }
        return -1;
    }

    @Override
    public void disconnectClient(String port, long clientId) throws RemoteException {
        ChannelDescriptor cd = null;
       /* for (Map.Entry<ChannelDescriptor, String> entry : WorldRegistryImpl.getInstance().getChannelServerStorage().getPorts().entrySet()) {
            if (entry.getValue().equals(port)) {
                cd = entry.getKey();
                break;
            }
        }
        if (cd != null) {
            WorldRegistryImpl.getInstance().getChannel(cd).disconnectClient(clientId);
        }*/
    }
}
