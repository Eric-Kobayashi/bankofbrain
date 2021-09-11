import qupath.lib.gui.scripting.QPEx
import qupath.lib.images.servers.ImageServer
import qupath.lib.scripting.QP
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane
import qupath.lib.regions.RegionRequest
import qupath.lib.images.servers.LabeledImageServer

import java.awt.image.BufferedImage

// Write the full image (only possible if it isn't too large!)
def P_SIZE = 0.2528
def imageData = QP.getCurrentImageData()
def server = QP.getCurrentServer()
def viewer = QPEx.getCurrentViewer()
def imagename = server.getPath().split('/').last().replace('.svs', '').replace('[--series, 0]', '')
def detections = "C:/Users/Eric/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/detection_stardist"
def cell_info = detections + '/' + imagename + '.svs_predicted.txt'
def linenum = 0

def is = new Scanner(new File(cell_info))
ArrayList<Double> cells_x = new ArrayList<Double>();
ArrayList<Double> cells_y = new ArrayList<Double>();
ArrayList<String> class_c = new ArrayList<String>();
while (is.hasNextLine()) {
    acell = is.nextLine()
    if (linenum > 0) {
        cells_x.add(Double.parseDouble(acell.split('\t')[2]))
        cells_y.add(Double.parseDouble(acell.split('\t')[3]))
        class_c.add(acell.split('\t')[1])
        linenum++;
    }
}

def cellnum = cells_x.size()
def i = 0
QP.getDetectionObjects().each { detection ->
    i = 0
    while (i < cellnum) {
        if ((detection.getROI().getCentroidX() - cells_x[i] < 0.1) && (detection.getROI().getCentroidY() - cells_y[i] < 0.1)) {
            detection.setPathClass(QP.getPathClass(class_c[i]))
            break
        }
        i++
    }
}


def labelServer = new LabeledImageServer.Builder(imageData)
       .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
       .downsample(1)    // Choose server resolution; this should match the resolution at which tiles are exported
       .lineThickness(2)          // Optionally export annotation boundaries with another label
       .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
       .build()

def cellnum = cells_x.size()
def i = 0
def windowsize = 100
def save_path = "C:/Users/Eric/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/"
while (i < cellnum) {
   plane = ImagePlane.getPlane(0, 0)
   xroi = Math.round(cells_x[i]-windowsize/2)
   yroi = Math.round(cells_y[i]-windowsize/2)
   roi = ROIs.createRectangleROI(xroi, yroi, windowsize, windowsize, plane)
   requestROI = RegionRequest.createInstance(server.getPath(), 1, roi)
//    ImageWriterTools.writeImageRegionWithOverlay(img, imageData, overlayOptions, request, fileImageWithOverlay.getAbsolutePath())
   QPEx.writeRenderedImageRegion(viewer, requestROI, save_path + String.format('/cells/%s_%s.tif', imagename, i.toString()))
   i++
}
