
package net.sf.odinms.net.channel.handler;

import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;

public class DragonBall1Handler extends AbstractMaplePacketHandler{


    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        /*
         * n = (1<<(k[0]-1)) | (1<<(k[1]-1)) | ... | (1<<(k[m]-1))
         * 其中n是前面的数，k[i]对应后面的数，比如
         * 1 = (1<<(1-1)) //n = 1, k[0] = 1
         * 2 = (1<<(2-1)) //n = 1, k[0] = 2
         * 3 = (1<<(1-1)) | (1<<(2-1)) //n = 3, k[0] = 1, k[1] = 2
         * 4 = (1<<(3-1)) //n = 4, k[0] = 3
         * 5 = (1<<(1-1)) | (1<<(3-1)) //n = 5, k[0] = 1, k[1] = 3
         * 6 = (1<<(2-1)) | (1<<(3-1)) //n = 6, k[0] = 2, k[i+1] = 3
         * 7 = (1<<(1-1)) | (1<<(2-1)) | (1<<(3-1)) //n = 7, k[0] = 1, k[1] = 2, k[2] = 3
         * ...
         * 70 = (1<<(2-1)) | (1<<(3-1)) | (1<<(7-1)) //n = 70, k[0] = 2, k[1] = 3, k[2] = 7
         * 换句话说，也就是 n = 2(k[0]-1次方) + 2(k[1]-1次方) + ... + 2(k[m]-1次方)
         */
        int i = 0;
        int a = 0;
        int d = 0;
        int f = 0;
        List<Integer> b = new ArrayList<Integer>(); //储存龙珠的排列 从小到大
        while(i < 9){
            d = 3994200 + i;
            a = c.getPlayer().getItemAmount(d);
            i += 1;
            if(a > 0)
                b.add(i);
        }
        if(b.size() > 0 && b.size() < 9){
            for(int e : b){
                f |= (1 << (e - 1));
            }
            c.getSession().write(MaplePacketCreator.DragonBall1(f,false));
        } else {
            c.getSession().write(MaplePacketCreator.DragonBall1(f,true));//这里的f值无意义了。
        }
    }
}
