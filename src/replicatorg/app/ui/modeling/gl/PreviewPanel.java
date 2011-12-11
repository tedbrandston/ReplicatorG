/**
 * 
 */
package replicatorg.app.ui.modeling.gl;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLJPanel;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.MainWindow;
import replicatorg.app.ui.modeling.AbstractEditingModel;
import replicatorg.app.ui.modeling.AbstractPreviewPanel;
import replicatorg.app.ui.modeling.ToolPanel;
import replicatorg.model.AbstractBuildModel;
import replicatorg.model.gl.Scene;
import replicatorg.model.gl.Shape;

/*
 * Okay, so, for these changes to opengl 
 * what will my New stucture look like?
 * what does the Old structure look like?
 * what needs to Change to ease the introduction of build plates?
 * 
 * Old:
 * PreviewPanel contains a 3D Canvas on which things get displayed?
 * 		displays background grid, axes, etc
 * 		displays the editing model
 * EditingModel creates and supplies a 'BranchGroup' to be displayed
 * 		only has one STL
 * 		has various transformation functions
 * replicatorg.model.BuildModelGL seems to do the construction of the 3d model
 * 		or is it the src/org/j3d/renderer/java3d/loaders?
 * 
 * Change:
 * we should probably use BuildModelGL as the class that wraps our new display lists
 * 		we'll need to set it up to allow multiple build models?
 * 		or add a BuildPlate (extends BuildElement) that contains BuildModels
 * 			perhaps this also keeps track of what's selected, etc.
 * 		based on Far's SmartBuildModel, I should
 * 			keep BuildModelGL as it is,
 * 			add a SmartModel
 * 			add a BuildPlate, which holds SmartModels
 * how do I deal with selecting different parts of the model?
 * 		PreviewPanel will have to keep track of the selected model
 * 			and all of the transformations will have to be applied to individual display lists
 * 			so display lists should probably be wapped in some class that can transform them, etc.
 * 		alternatively, BuildPlate keeps track of the selected model and passes transformations to it
 * So, we can differentiate between having an stl and an stls open,
 * 		we can add a 'new .stls' option to the file menu
 * 		if an stls is open an new button appears "add stl" that opens an open dialog
 * 			opening one or more stl files will add them to the scene, and copy them into the stls (maybe on save)
 * 			moving them around, rotating, etc. will change the metadata about them, which can then be saved to the stls
 * 
 * New:
 * the canvas becomes a GLJPanel
 * 		I don't think it needs to be animated
 * 		but I do need to have a GLEventListener to init, display, etc.
 * I know opengl has some way of creating and storing models (see display lists, example: http://www.java-tips.org/other-api-tips/jogl/how-to-create-a-display-list-in-jogl.html
 * 		EditingModel should store each STL as a display list
 * 		PreviewPanel can now display each display list
 * 		if(EditingModel.hasMultipleItems()) can only save as '.stls'
 * everything in src/org/j3d/renderer/java3d/loaders is used to turn loaded files into j3d Scenes
 * 		that will all have to be repurposed (or removed) 
 * 
 * 
 * things that methods of this class do:
 * 
	//
	EditingModel getModel();
	//
	void setModel(BuildModelGL buildModel);
	//
	void setScene(EditingModel model);
	//
	void getBuildVolume();
	
	// called when loading a new machine, should probably be called switchMachine
	// or wrapped in a function switchMachine
	void rebuildScene();
	
	//
	Node makeAmbientLight();
	//
	Node makeDirectedLight1();
	//
	Node makeDirectedLight2();
	
	// Draws some text to some point in the scene
	// could switch it to pake a point, transform that point, and do raster text there
	Group makeLabel(String s, Vector3d where);
	
	//is this necessary?
	void loadPoint(Point3d point, double[] array, int idx);
	
	//
	Shape3D makeBoxFrame(Point3d ll, Vector3d dim);
	//
	Shape3D makePlatform(Point3d lower, Point3d upper);
	//
	Group makeAxes(Point3d origin);
	//
	Node makeBoundingBox();
	// glSetClearColor()
	Node makeBackground();
	//
	Node makeBaseGrid();
	// this could be done better, more realistically
	Node makeStarField(int pointCount, int pointSize);
	
	// should be renamed makeScene, incorporates everything above plus our model
	BranchGroup createSTLScene();
	
	//Center the object and flatten the bottommost poly.  (A more thorough version would
	//be able to correctly center a tripod or other spiky object.)
	void align();
	// re-calculates the view matrix
	void updateVP();
	// replace with initGL()?
	Canvas3D createUniverse();

	//
	void viewXY();
	//
	void viewYZ();
	//
	void viewXZ();
	
	// perspective vs. ortho
	void usePerspective(boolean perspective);
 * 
 * 
 * 
 * 
 */
public class PreviewPanel extends AbstractPreviewPanel {
	
	/** This holds everything we're going to display:
	 * The platform, the starfield, the model object(s), etc.,
	 * including the objectScene
	 */
	Scene scene;
	/**
	 * This holds the model's objects
	 */
	Scene objectScene;
	
	/**
	 *  our GLContext on which all lighting/rendering is done 
	 */
	GLContext context;
	
	public PreviewPanel(final MainWindow mainWindow) {
		super(mainWindow);
		setLayout(new MigLayout("fill,ins 0,gap 0"));

		canvas = new GLJPanel();
		
		initGL();
		
		add(canvas, "growx,growy");
		
		toolPanel = new ToolPanel(this);
		if (Base.isMacOS()) {
			add(toolPanel,"dock east,width max(300,25%)");
		} else {
			add(toolPanel,"dock east,width max(200,20%)");
		}
		// Create the content branch and add it to the universe
		refreshScenery();
		
		canvas.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				updateViewPoint();
			}
		});
		
		addKeyListener(toolPanel);

	}

	@Override
	public void setBuildModel(AbstractBuildModel buildModel) {
		if (model == null || buildModel != model.getBuildModel()) {
			if (buildModel != null) {
				model = new EditingModel((replicatorg.model.gl.BuildModel)buildModel, mainWindow);
				setEditingModel(model);
			} else {
				model = null;
			}
		}
	}

	@Override
	public void setEditingModel(AbstractEditingModel editingModel) {
		Base.logger.info(editingModel.getBuildModel().getPath());
		
		// refers to our package's EditingModel
		if(! (editingModel instanceof EditingModel))
			throw new IllegalArgumentException("PreviewPanel requires a compatible EditingModel");
		
		EditingModel eModel = (EditingModel)editingModel;
		
		if (objectScene != null) {
			scene.remove(objectScene);
		}
		objectScene = eModel.getScene();
		scene.add(objectScene);
	}
	
	/*
	 * This is to ensure we can switch between machines with different dimensions
	 * It is called from MainWindow loadMachine()
	 */
	@Override
	public void refreshScenery() {

		getBuildVolume();
		
		makeAmbientLight();
		makeDirectedLights();
		
		scene.add(makeBoundingBox());
		scene.add(makeBackground());
		scene.add(makeBaseGrid());
		
		if(Base.preferences.getBoolean("ui.show_starfield", false)) {
			scene.add(makeStarField(400, 2));
		}
	}
	
	@Override
	public void refreshObjects() {
		if (objectScene != null) {
			scene.remove(objectScene);
		}
		objectScene = ((EditingModel)model).getScene();
		model.updateModelColor();
		scene.add(objectScene);
	}
	
	@Override
	public void updateViewPoint() {
//		TransformGroup viewTG = univ.getViewingPlatform().getViewPlatformTransform();
		
		// we init all the matrices using the identity matrix, to save calls to setIdentity
		viewMatrix = new Matrix4d();
		viewMatrix.setIdentity();
		Matrix4d trans = new Matrix4d(viewMatrix);
		Matrix4d rotZ = new Matrix4d(viewMatrix);
		Matrix4d rotX = new Matrix4d(viewMatrix);
		Matrix4d drop = new Matrix4d(viewMatrix);
		Matrix4d raise = new Matrix4d(viewMatrix);
		
		trans.setTranslation(cameraTranslation);
		drop.setTranslation(new Vector3d(0,0,50)); // magic number?
		raise.invert(drop);
		
		rotX.rotX(elevationAngle);
		rotZ.rotZ(turntableAngle);
		
		viewMatrix.mul(drop);
		viewMatrix.mul(rotZ);
		viewMatrix.mul(rotX);
		viewMatrix.mul(raise);
		viewMatrix.mul(trans);

		if (Base.logger.isLoggable(Level.FINE)) {
			Base.logger.fine("Camera Translation: "+cameraTranslation.toString());
			Base.logger.fine("Elevation "+Double.toString(elevationAngle)+", turntable "+Double.toString(turntableAngle));
		}
	}
	
	public void makeAmbientLight() {
//		AmbientLight ambient = new AmbientLight();
////		ambient.setColor(new Color3f(0.3f,0.3f,0.9f));
//		ambient.setColor(new Color3f(1f,1f,1f));
//		ambient.setInfluencingBounds(bounds);
//		return ambient;
		GL2 gl = context.getGL().getGL2();
		
		float[] ambient_light = new float[]{.5f, .5f, .5f, 1f};
		gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, ambient_light, 0);
	}

	public void makeDirectedLights() {
//		Color3f color = new Color3f(0.7f,0.7f,0.7f);
//		Vector3f direction = new Vector3f(1f,0.7f,-0.2f);
//		DirectionalLight light = new DirectionalLight(color,direction);
//		light.setInfluencingBounds(bounds);
//		return light;
//	}
//
//	public void makeDirectedLight2() {
//		Color3f color = new Color3f(0.5f,0.5f,0.5f);
//		Vector3f direction = new Vector3f(-1f,-0.7f,-0.2f);
//		DirectionalLight light = new DirectionalLight(color,direction);
//		light.setInfluencingBounds(bounds);
//		return light;

		//GL: see http://www.felixgers.de/teaching/jogl/lightProg.html
		
		//White light, white heat
//		float[] white = new float[]{1f, 1f, 1f};
		
		GL2 gl = context.getGL().getGL2();
		float[] light0Color = new float[]{0.7f,0.7f,0.7f, 1};
		float[] light0Direction = new float[]{1f,0.7f,-0.2f, 0};
		//I've just inverted this, because I have no Idea what to do
		float[] light0Position = new float[]{-1f,-0.7f,0.2f};
		
		float[] light1Color = new float[]{0.5f,0.5f,0.5f, 1};
		float[] light1Direction = new float[]{-1f,-0.7f,-0.2f, 0};
		//I've just inverted this, because I have no Idea what to do
		float[] light1Position = new float[]{1f,0.7f,0.2f};

		gl.glEnable(GL2.GL_LIGHT0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light0Position, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPOT_DIRECTION, light0Direction, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, light0Color, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, light0Color, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, light0Color, 0);
		
		gl.glEnable(GL2.GL_LIGHT1);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, light1Position, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPOT_DIRECTION, light1Direction, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, light1Color, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, light1Color, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, light1Color, 0);
//		throw new UnsupportedOperationException("Hey! Who turned out the lights?");
	}

	final double wireBoxCoordinates[] = {
			0,  0,  0,    0,  0,  1,
			0,  1,  0,    0,  1,  1,
			1,  1,  0,    1,  1,  1,
			1,  0,  0,    1,  0,  1,

			0,  0,  0,    0,  1,  0,
			0,  0,  1,    0,  1,  1,
			1,  0,  1,    1,  1,  1,
			1,  0,  0,    1,  1,  0,

			0,  0,  0,    1,  0,  0,
			0,  0,  1,    1,  0,  1,
			0,  1,  1,    1,  1,  1,
			0,  1,  0,    1,  1,  0,
	};

	public Shape makeBoxFrame(Point3d ll, Vector3d dim) {
		Shape frame = new Scene(new ArrayList<Shape>());
		double thickness = .001;
//		double[] coords = new double[wireBoxCoordinates.length];
//		for (int i = 0; i < wireBoxCoordinates.length;) {
//			coords[i] = (wireBoxCoordinates[i] * dim.x) + ll.x; i++;
//			coords[i] = (wireBoxCoordinates[i] * dim.y) + ll.y; i++;
//			coords[i] = (wireBoxCoordinates[i] * dim.z) + ll.z; i++;
//		}
		
		//for each pair of coordinates I want to draw eight triangles (four quads)
		//There are 12 lines, with 8 triangles each = 96 triangles
//		Point3d[][] triangles = new Point3d[96][3];
//		for(int i = 0; i < wireBoxCoordinates.length; i += 6)
//		{
//			triangles[i][0] = new Point3d()
//		}
//		
//		for(int j = 0; j < triangles.length; j++)
//		{
//			triangles[j][0].x *= dim.x;
//			triangles[j][0].x += ll.x;
//			triangles[j][0].y *= dim.y;
//			triangles[j][0].y += ll.y;
//			triangles[j][0].z *= dim.z;
//			triangles[j][0].z += ll.z;
//		}
//
		return frame; 
		
		
		/*
		 * this seems excessive, I'll wait until I can use quads?
		 */
		
	}

//	Font3D labelFont = null;
	
	public void makeLabel(String s, Vector3d where) {
//		if (labelFont == null) {
//			labelFont = new Font3D(Font.decode("Sans"), new FontExtrusion());
//		}
//		Text3D text = new Text3D(labelFont, s);
//        TransformGroup tg = new TransformGroup();
//        Transform3D transform = new Transform3D();
//        transform.setTranslation(where);
//        tg.setTransform(transform);
//        OrientedShape3D os = new OrientedShape3D();
//        os.setAlignmentAxis( 0.0f, 0.0f, 1.0f);
//        os.setAlignmentMode(OrientedShape3D.ROTATE_ABOUT_POINT);
//        os.setConstantScaleEnable(true);
//        os.setScale(0.05);
//        os.setGeometry(text);
//        tg.addChild(os);
		System.out.println("Make label not implemented");
		// take the point 'where', project it to the screen,
		// push the matrix (not the modelview, the other one) switch to flat, 
		// no projection. use gl text functions
	}
	public Shape makeAxes() {
		Shape axes = null;
//		g.addChild(makeLabel("X",new Vector3d(57,0,0)));
//		g.addChild(makeLabel("Y",new Vector3d(0,57,0)));
//		g.addChild(makeLabel("Z",new Vector3d(0d,0d,107)));
		System.out.println("make axes not implemented");
		return axes;
	}
	
//	Seriously? this is a function? it's called four times.
//	private void loadPoint(Point3d point, double[] array, int idx) {
//		array[idx] = point.x;
//		array[idx+1] = point.y;
//		array[idx+2] = point.z;
//	}
		
	private Shape makePlatform(Point3d lower, Point3d upper) {
		Shape platform = null;
//		Color3f color = new Color3f(.05f,.35f,.70f); 
//		ColoringAttributes ca = new ColoringAttributes();
//		ca.setColor(color);
//		Appearance solid = new Appearance();
//		solid.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST,0.13f));
//		//solid.setMaterial(m);
//		solid.setColoringAttributes(ca);
//		PolygonAttributes pa = new PolygonAttributes();
//		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
//		pa.setCullFace(PolygonAttributes.CULL_NONE);
//		pa.setBackFaceNormalFlip(true);
//	    solid.setPolygonAttributes(pa);
//
//		double[] coords = new double[4*3];
//		loadPoint(lower,coords,0);
//		loadPoint(new Point3d(lower.x,upper.y,upper.z),coords,3);
//		loadPoint(upper,coords,6);
//		loadPoint(new Point3d(upper.x,lower.y,upper.z),coords,9);
//			
//		QuadArray plat = new QuadArray(4,GeometryArray.COORDINATES);
//		plat.setCoordinates(0, coords);

		System.out.println("make platform not supported");
		
		return platform; 
		
	}

	
	public Shape makeBoundingBox() {

		Shape bounds = null;
//		Group boxGroup = new Group();
//		// TODO: Change these dimensions if it has a custom buid-volume! Display cut-outs?!
//		// Same for the platform
//		if(buildVol == null)
//		{
//			Shape3D boxframe = makeBoxFrame(new Point3d(-50,-50,0), new Vector3d(100,100,100));
//			boxGroup.addChild(boxframe);
//			boxGroup.addChild(this.makePlatform(new Point3d(-50,-50,-0.001), new Point3d(50,50,-0.001)));
//		} else {
//			Vector3d boxdims = new Vector3d(buildVol.getX(),buildVol.getY(),buildVol.getZ());
//			Shape3D boxframe = makeBoxFrame(new Point3d((int) -buildVol.getX()/2,(int) -buildVol.getY()/2,0), boxdims);	
//			boxGroup.addChild(boxframe);
//			boxGroup.addChild(this.makePlatform(new Point3d((int) -buildVol.getX()/2,(int) -buildVol.getY()/2,-0.001), new Point3d(buildVol.getX()/2,buildVol.getY()/2,-0.001)));
//		}
			
	
			/*
			Appearance sides = new Appearance();
			sides.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST,0.9f));
			Color3f color = new Color3f(0.05f,0.05f,1.0f); 
			Material m = new Material(color,color,color,color,64.0f);
			sides.setMaterial(m);
	
			Box box = new Box(50,50,50,sides);
			Transform3D tf = new Transform3D();
			tf.setTranslation(new Vector3d(0,0,50));
			TransformGroup tg = new TransformGroup(tf);
			tg.addChild(box);
			tg.addChild(boxframe);
			*/
		System.out.println("make bounding box not implemented");
		return bounds;
	}

	public Shape makeBackground() {
		Shape background = null;
//		Color backgroundColor = new Color(Base.preferences.getInt("ui.backgroundColor", 0));
//		Background bg = new Background(backgroundColor.getRed()/255f, backgroundColor.getGreen()/255f, backgroundColor.getBlue()/255f);
//		bg.setApplicationBounds(bounds);
		System.out.println("make background not implemented");
		return background;
	}
	
    public Shape makeBaseGrid() {
    	Shape grid = null;
//    	if(buildVol instanceof BuildVolume)
//    	{
//    		Group baseGroup = new Group();
//    		double gridSpacing = 10.0; // Dim grid has hash marks at 10mm intervals.
//    		// Set up the appearance object for the central crosshairs.
//	        Appearance crosshairAppearance = new Appearance();
//	        crosshairAppearance.setLineAttributes(new LineAttributes(3,LineAttributes.PATTERN_SOLID,true));
//	        crosshairAppearance.setColoringAttributes(new ColoringAttributes(.9f,1f,1f,ColoringAttributes.NICEST));
//	        // Set up the crosshair lines
//	        LineArray crosshairLines = new LineArray(2*2,GeometryArray.COORDINATES);
//	        crosshairLines.setCoordinate(0, new Point3d(0,-buildVol.getY()/2,0));
//	        crosshairLines.setCoordinate(1, new Point3d(0,buildVol.getY()/2,0));
//	        crosshairLines.setCoordinate(2, new Point3d(-buildVol.getX()/2,0,0));
//	        crosshairLines.setCoordinate(3, new Point3d(buildVol.getX()/2,0,0));
//	        Shape3D crosshairs = new Shape3D(crosshairLines,crosshairAppearance);
//
//    		// Set up the appearance object for the measurement hash marks.
//	        Appearance hashAppearance = new Appearance();
//	        hashAppearance.setLineAttributes(new LineAttributes(2f,LineAttributes.PATTERN_SOLID,true));
//	        hashAppearance.setColoringAttributes(new ColoringAttributes(.475f,.72f,.85f,ColoringAttributes.NICEST));
//	        // hashes in each direction on x axis
//	        int xHashes = (int)((buildVol.getX() - 0.0001)/(2*gridSpacing));
//	        // hashes in each direction on y axis
//	        int yHashes = (int)((buildVol.getY() - 0.0001)/(2*gridSpacing));
//	        // Set up hash lines
//	        LineArray hashLines = new LineArray(2*(2*xHashes + 2*yHashes),GeometryArray.COORDINATES);
//	        int idx = 0;
//        	double offset = 0;
//	        for (int i = 0; i < xHashes; i++) {
//	        	offset += gridSpacing;
//                hashLines.setCoordinate(idx++, new Point3d(offset,-buildVol.getY()/2,0));
//                hashLines.setCoordinate(idx++, new Point3d(offset,buildVol.getY()/2,0));             
//                hashLines.setCoordinate(idx++, new Point3d(-offset,-buildVol.getY()/2,0));
//                hashLines.setCoordinate(idx++, new Point3d(-offset,buildVol.getY()/2,0));
//	        }
//	        offset = 0;
//	        for (int i = 0; i < yHashes; i++) {
//	        	offset += gridSpacing;
//                hashLines.setCoordinate(idx++, new Point3d(-buildVol.getX()/2,offset,0));
//                hashLines.setCoordinate(idx++, new Point3d(buildVol.getX()/2,offset,0));
//                hashLines.setCoordinate(idx++, new Point3d(-buildVol.getX()/2,-offset,0));
//                hashLines.setCoordinate(idx++, new Point3d(buildVol.getX()/2,-offset,0));
//	        }
//	        Shape3D hashes = new Shape3D(hashLines,hashAppearance);
//	        
//	        baseGroup.addChild(hashes);
//	        baseGroup.addChild(crosshairs);
//	        return baseGroup;
//    	} return null;
    	System.out.println("make base grid not implemented");
    	return grid;
    }
    
    public Shape makeStarField(int pointCount, int pointSize) {
    	// Oh! I feel it! I feel the Cosmos!
    	Shape stars = null;
//        BranchGroup bg = new BranchGroup();
//
//        PointArray pointArray = new PointArray(pointCount * pointCount,
//            GeometryArray.COORDINATES | GeometryArray.COLOR_3);
//
//        int nPoint = 0;
//
//        Random rand = new Random();
//        
//        for (int n = 0; n < pointCount; n++) {
//        	// Choose a random point in the cosmos.
//        	double r = 1000;
//        	double theta = rand.nextFloat()*2*Math.PI;
//        	double phi = rand.nextFloat()*2*Math.PI;
//        	
//        	double x = r * Math.sin(theta) * Math.cos(phi);
//        	double y = r * Math.sin(theta) * Math.sin(phi);
//        	double z = r * Math.cos(theta);
//
//            Point3f point = new Point3f((float)x, (float)y, (float)z);
//            pointArray.setCoordinate(nPoint, point);
//            pointArray.setColor(nPoint++, new Color3f(1f, 1f, 1f));
//        }
//
//        // create the material for the points
//        Appearance pointApp = new Appearance();
//
//        // enlarge the points
//        pointApp.setPointAttributes(new PointAttributes(pointSize, true));
//
//        Shape3D pointShape = new Shape3D(pointArray, pointApp);
//
//        bg.addChild(pointShape);
    	System.out.println("make stars not yet implemented");
        return stars;
    }

	/*******************************************************************************************************************************/

	Matrix4d viewMatrix;

	@Override
	public Matrix4d getViewTransform() {
//		TransformGroup viewTG = univ.getViewingPlatform().getViewPlatformTransform();
//		Transform3D t = new Transform3D();
//		viewTG.getTransform(t);
//		return t;
//		throw new UnsupportedOperationException("PreviewPanel.getViewTransform() not implemented");
		return viewMatrix;
	}
	
	public void usePerspective(boolean perspective) {
//		univ.getViewer().getView().setProjectionPolicy(perspective?View.PERSPECTIVE_PROJECTION:View.PARALLEL_PROJECTION);
		
	}

	private void initGL() {
//		// Get the preferred graphics configuration for the default screen
//		GraphicsConfiguration config =
//			SimpleUniverse.getPreferredConfiguration();
//
//		// Create a Canvas3D using the preferred configuration
//		Canvas3D c = new Canvas3D(config) {
//			public Dimension getMinimumSize()
//		    {
//		        return new Dimension(0, 0);
//		    }
//		};
//
//		// Create simple universe with view branch
//		univ = new SimpleUniverse(c);
//		univ.getViewer().getView().setSceneAntialiasingEnable(true);
//		univ.getViewer().getView().setFrontClipDistance(10d);
//		univ.getViewer().getView().setBackClipDistance(1000d);
//		updateViewPoint();
//
//		// Ensure at least 5 msec per frame (i.e., < 200Hz)
//		univ.getViewer().getView().setMinimumFrameCycleTime(5);
//
//		return c;

		if(context == null)
			context = GLContext.getCurrent();

		GL2 gl = context.getGL().getGL2();
		
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);
		
	}
	
}
