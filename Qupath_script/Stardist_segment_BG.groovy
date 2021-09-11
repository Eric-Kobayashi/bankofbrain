import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP
import qupath.tensorflow.stardist.StarDist2D

def imagename = getCurrentServer().getPath().split('/').last().replace('.svs[--series, 0]', '')
for (ann in QP.getAnnotationObjects()) {
    if (ann.getPathClass() != null) {
        if (ann.getPathClass().getName() == 'GP') {
            ann_GP = ann
        } else if (ann.getPathClass().getName() == 'STR') {
            ann_STR = ann
        } else if (ann.getPathClass().getName() == 'STN') {
            ann_STN = ann
        }
    } else {
        ann_Tissue = ann
    }
}

try {
    QPEx.setSelectedObject(ann_Tissue)
} catch (Exception ignored) {
    // Slides excluded from analysis
    print(imagename+': No annotation')
    return
}

GP_flag = 1
try {
    QPEx.setSelectedObject(ann_GP)
} catch (Exception ignored) {
    // Slides excluded from analysis
    GP_flag = 0
}

STR_flag = 1
try {
    QPEx.setSelectedObject(ann_STR)
} catch (Exception ignored) {
    // Slides excluded from analysis
    STR_flag = 0
}

STN_flag = 1
try {
    QPEx.setSelectedObject(ann_STN)
} catch (Exception ignored) {
    // Slides excluded from analysis
    STN_flag = 0
}

if ((STN_flag == 0) && (GP_flag == 0) && (STR_flag == 0)) {
    return
}

//QP.clearDetections()

// Specify the model directory (you will need to change this!)
// def pathModel = 'F:/Stardist/dsb2018_heavy_augment'
def pathModel = 'F:/Stardist/he_heavy_augment'

def stardist = StarDist2D.builder(pathModel)
        .threshold(0.5)            // Prediction threshold
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(0.25)              // Resolution for detection
        .cellExpansion(5.0)          // Approximate cells based upon nucleus expansion
        .cellConstrainScale(3)     // Constrain cell expansion using nucleus size
        .ignoreCellOverlaps(false)   // Set to true if you don't care if cells expand into one another
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
        .includeProbability(true)    // Add probability as a measurement (enables later filtering)
        .nThreads(8)                 // Limit the number of threads used for (possibly parallel) processing
        .simplify(1)                 // Control how polygons are 'simplified' to remove unnecessary vertices
        .doLog()                     // Use this to log a bit more information while running the script
        .build()


// Run detection for the selected objects
def imageData = getCurrentImageData()
QPEx.setSelectedObject(ann_Tissue)
def pathObjects = getSelectedObjects()

stardist.detectObjects(imageData, pathObjects)

QP.getDetectionObjects().each { detection ->
    droi = detection.getROI()
    if ((GP_flag == 1) && (ann_GP.getROI().contains(droi.getCentroidX(), droi.getCentroidY()))) {
        detection.setName("Globus Pallidus")
    } else if ((STR_flag == 1) && (ann_STR.getROI().contains(droi.getCentroidX(), droi.getCentroidY()))) {
        detection.setName("Striatum")
    } else if ((STN_flag == 1) && (ann_STN.getROI().contains(droi.getCentroidX(), droi.getCentroidY()))) {
        detection.setName("Subthalamic Nucleus")
    } else {
        detection.setName("Doesnt_matter")
    }
}

// Create nearest neighbour features (50 um and 100 um) and save
//QPEx.saveDetectionMeasurements(String.format('/E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/detection_stardist/%s_all.txt', imagename))
