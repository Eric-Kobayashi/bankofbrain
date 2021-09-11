import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP
import qupath.tensorflow.stardist.StarDist2D

def imagename = getCurrentServer().getPath().split('/').last().replace('.svs[--series, 0]', '')
//QP.clearDetections()

for (ann in QP.getAnnotationObjects()) {
    if (ann.getPathClass() != null) {
        if (ann.getPathClass().getName() == 'Tumor') {
            ann_WM = ann
        } else if (ann.getPathClass().getName() == 'Stroma') {
            ann_GM = ann
        } else if (ann.getPathClass().getName() == 'Immune cells') {
            ann_GMGC = ann
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

QP.setColorDeconvolutionStains('{"Name" : "H-DAB trial", "Stain 1" : "Hematoxylin", "Values 1" : "0.651 0.701 0.290 ", "Stain 2" : "DAB", "Values 2" : "0.269 0.568 0.778 ", "Background" : " 255 255 255 "}')

cbcflag = 1
try {
    QPEx.setSelectedObject(ann_GMGC)
} catch (Exception ignored) {
    // Not cerebellum
    cbcflag = 0
}

wmflag = 1
try {
    QPEx.setSelectedObject(ann_WM)
} catch (Exception ignored) {
    // Not segmented into GM and WM
    wmflag = 0
}

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

// Cerebellum
if (cbcflag == 1) {
    QP.getDetectionObjects().each { detection ->
        droi = detection.getROI()
        if (ann_WM.getROI().contains(droi.getCentroidX(), droi.getCentroidY())) {
            detection.setName("White_matter")
        } else if (ann_GMGC.getROI().contains(droi.getCentroidX(), droi.getCentroidY())) {
            detection.setName("Grey_matter_Granular")
        } else if (ann_GM.getROI().contains(droi.getCentroidX(), droi.getCentroidY())) {
            detection.setName("Grey_matter")
        } else {
            detection.setName("Doesnt_matter")
        }
    }
}
// Slides that annotates white matter first
else if (wmflag == 1) {
    QP.getDetectionObjects().each { detection ->
        droi = detection.getROI()
        if (ann_WM.getROI().contains(droi.getCentroidX(), droi.getCentroidY())) {
            detection.setName("White_matter")
        } else if (ann_GM.getROI().contains(droi.getCentroidX(), droi.getCentroidY())) {
            detection.setName("Grey_matter")
        } else {
            detection.setName("Doesnt_matter")
        }
    }
}

// Slides that are not segmented into white/grey matter
else {
    QP.getDetectionObjects().each { detection ->
        detection.setName("Doesnt_matter")
    }
}
// Create nearest neighbour features (50 um and 100 um) and save
QPEx.saveDetectionMeasurements(String.format('/E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/detection_stardist/%s_all.txt', imagename))
