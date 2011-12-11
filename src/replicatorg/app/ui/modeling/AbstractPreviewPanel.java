package replicatorg.app.ui.modeling;

import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import replicatorg.app.Base;
import replicatorg.app.ui.MainWindow;
import replicatorg.machine.Machine;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.model.BuildVolume;
import replicatorg.machine.model.MachineModel;
import replicatorg.model.AbstractBuildModel;

public abstract class AbstractPreviewPanel extends JPanel {

	protected final MainWindow mainWindow;
	protected Tool currentTool = null;
	protected AbstractEditingModel model = null;
	protected Component canvas;
	
	protected BuildVolume buildVol;
	protected ToolPanel toolPanel;

	// These values were determined experimentally to look pretty dang good.
	protected final Vector3d CAMERA_TRANSLATION_DEFAULT = new Vector3d(0,0,290);
	protected final double ELEVATION_ANGLE_DEFAULT = 1.278;
	protected final double TURNTABLE_ANGLE_DEFAULT = 0.214;
	protected final double CAMERA_DISTANCE_DEFAULT = 300d; // 30cm
	
	protected Vector3d cameraTranslation = new Vector3d(CAMERA_TRANSLATION_DEFAULT);
	protected double elevationAngle = ELEVATION_ANGLE_DEFAULT;
	protected double turntableAngle = TURNTABLE_ANGLE_DEFAULT;

	public AbstractPreviewPanel(final MainWindow mainWindow) {
		super(true);
		this.mainWindow = mainWindow;
	}

	public AbstractPreviewPanel(final MainWindow mainWindow, boolean isDoubleBuffered) {
		super(isDoubleBuffered);
		this.mainWindow = mainWindow;
	}

	public AbstractEditingModel getEditingModel() {
		return model;
	}
	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public void getBuildVolume(){
		Base.logger.fine("Resetting the build volume!");
		MachineInterface mc = this.mainWindow.getMachine(); 
		if(mc instanceof Machine){
			MachineModel mm = mc.getModel();
			buildVol = mm.getBuildVolume();
			Base.logger.fine("Dimensions:" + buildVol.getX() +','+ buildVol.getY() + ',' + buildVol.getZ());
		}
	}
	
	public void setTool(Tool tool) {
		if (currentTool == tool) { return; }
		if (currentTool != null) {
			if (currentTool instanceof MouseListener) {
				canvas.removeMouseListener((MouseListener)currentTool);
			}
			if (currentTool instanceof MouseMotionListener) {
				canvas.removeMouseMotionListener((MouseMotionListener)currentTool);
			}
			if (currentTool instanceof MouseWheelListener) {
				canvas.removeMouseWheelListener((MouseWheelListener)currentTool);
			}
			if (currentTool instanceof KeyListener) {
				canvas.removeKeyListener((KeyListener)currentTool);
			}
		}
		currentTool = tool;
		if (currentTool != null) {
			if (currentTool instanceof MouseListener) {
				canvas.addMouseListener((MouseListener)currentTool);
			}
			if (currentTool instanceof MouseMotionListener) {
				canvas.addMouseMotionListener((MouseMotionListener)currentTool);
			}
			if (currentTool instanceof MouseWheelListener) {
				canvas.addMouseWheelListener((MouseWheelListener)currentTool);
			}
			if (currentTool instanceof KeyListener) {
				canvas.addKeyListener((KeyListener)currentTool);
			}
		}
	}
	
	public void adjustViewAngle(double deltaYaw, double deltaPitch) {
		turntableAngle += deltaYaw;
		elevationAngle += deltaPitch;
		updateViewPoint();
	}

	public void adjustViewTranslation(double deltaX, double deltaY) {
		cameraTranslation.x += deltaX;
		cameraTranslation.y += deltaY;
		updateViewPoint();
	}

	public void adjustZoom(double deltaZoom) {
		cameraTranslation.z += deltaZoom;
		updateViewPoint();
	}

	public void resetView() {
		elevationAngle = ELEVATION_ANGLE_DEFAULT;
		turntableAngle = TURNTABLE_ANGLE_DEFAULT;
		updateViewPoint();
	}

	public void viewXY() {
		turntableAngle = 0d;
		elevationAngle = 0d;
		updateViewPoint();	
	}

	public void viewYZ() {
		turntableAngle = Math.PI/2;
		elevationAngle = Math.PI/2;
		updateViewPoint();	
	}

	public void viewXZ() {
		turntableAngle = 0d;
		elevationAngle = Math.PI/2;
		updateViewPoint();	
	}
	
	/**
	* Center the object and flatten the bottommost poly.  (A more thorough version would
	* be able to correctly center a tripod or other spiky object.)
	*/
	public void align() {
		model.center();
	}
	
	public abstract Matrix4d getViewTransform();
	
	public abstract void setBuildModel(AbstractBuildModel buildModel);

	public abstract void setEditingModel(AbstractEditingModel editingModel);

	public abstract void refreshScenery();
	public abstract void refreshObjects();
	
	public abstract void updateViewPoint();
	
	public abstract void usePerspective(boolean perspective);

}