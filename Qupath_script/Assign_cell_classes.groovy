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
def delta = 0.1/P_SIZE
def imageData = QP.getCurrentImageData()
def server = QP.getCurrentServer()
def viewer = QPEx.getCurrentViewer()
def imagename = server.getPath().split('/').last().replace('.svs', '').replace('[--series, 0]', '')
def detections = "E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/fifth_run/prediction"
def cell_info = detections + '/' + imagename + '.svs_all_predictions.txt'
def linenum = 0

def is = new Scanner(new File(cell_info))
ArrayList<Double> cells_x = new ArrayList<Double>();
ArrayList<Double> cells_y = new ArrayList<Double>();
ArrayList<String> cells_c = new ArrayList<String>();
while (is.hasNextLine()) {
    acell = is.nextLine()
    if (linenum > 0) {
        cells_x.add(Double.parseDouble(acell.split('\t')[2])/P_SIZE)
        cells_y.add(Double.parseDouble(acell.split('\t')[3])/P_SIZE)
        cells_c.add(acell.split('\t')[1])
    }
    linenum++;
}

def cellnum = cells_x.size()

QP.getDetectionObjects().each { detection ->
    xc = detection.getROI().getCentroidX()
    yc = detection.getROI().getCentroidY()
    if (!(xc.isNaN())) {
        flag = 0
        i = 0
        cellnum = cells_x.size()
        while (i < cellnum) {
            if ((xc - cells_x[i] <  delta) && (yc - cells_y[i] <  delta)) {
                detection.setPathClass(QP.getPathClass(cells_c[i]))
                flag = 1
                break
            }
            i++
        }
        if (flag == 1) {
            cells_x.remove(i)
            cells_y.remove(i)
            cells_c.remove(i)
        }
    }
}
