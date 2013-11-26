package plugins.text;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import plugins.applis.SimpleAligneur.PlayerListener;
import speechreco.aligners.sphiinx4.Alignment;
import plugins.text.elements.Element_Mot;
import tools.audio.PlayerGUI;

/**
 * ce thread est créé à chaque fois que le play commence, puis il est détruit au premier stop du player
 * 
 * @author xtof
 *
 */
public class GriserWhilePlaying {
	private PlayerGUI player;
	private boolean stop=false;
	private TexteEditor edit;
	private Alignment align=null;
	Timer timer=null;

	public GriserWhilePlaying(PlayerGUI p, TexteEditor editor) {
		edit=editor;
		player=p;
	}

	public void setAlignement(Alignment al) {
		align=al;
		initthread();
	}
	
	public void killit() {
		stop=true;
	}

	private void initthread() {
		System.out.println("griser init timer "+stop+" "+player.isPlaying());
		timer = new Timer(100, new ActionListener() {
			float curtime=-1;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (stop) timer.stop();
				
				if (player.isPlaying()) {
					long curt = System.currentTimeMillis();
					long t0 = player.getTimePlayStarted();
					int curfr = PlayerListener.millisec2frame(curt-t0);
					// ajoute le debut du segment joué
					curfr += player.getRelativeStartingSec()*100;
					int segidx = align.getSegmentAtFrame(curfr);
					if (segidx>=0) {
						Element_Mot mot = edit.getListeElement().getMotAtSegment(segidx);
						if (mot!=null) edit.griseMot(mot);
					}
					curtime+=0.1;
				} else curtime=-1;
			}
		});
		timer.start();
	}
}
