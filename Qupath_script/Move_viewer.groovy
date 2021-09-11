import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane
import qupath.lib.regions.RegionRequest
import qupath.lib.gui.dialogs.Dialogs;


def P_SIZE = 0.2528
def mag = 200

def viewer = QPEx.getCurrentViewer()
//def imagename = server.getPath().split('/').last().replace('.svs', '').replace('[--series, 0]', '')
def cell_info = "E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/fourth_run/classification_results/train_4_slides_corrclassified_703484.svs_all.txt"
def is = new Scanner(new File(cell_info))
ArrayList<Double> cells_x = new ArrayList<Double>();
ArrayList<Double> cells_y = new ArrayList<Double>();
def flag = 1;
while(is.hasNextLine())
{
    acell = is.nextLine()
    if (flag == 0) {
        cells_x.add(Double.parseDouble(acell.split('\t')[1])/P_SIZE)
        cells_y.add(Double.parseDouble(acell.split('\t')[2])/P_SIZE)
    }
    else {flag = 0}
}

def cellnum = cells_x.size()
def i = 0

while (i < cellnum) {
    xroi = Math.round(cells_x[i])
    yroi = Math.round(cells_y[i])
    viewer.setMagnification(mag)
    viewer.setCenterPixelLocation(xroi, yroi)
    if (!Dialogs.showYesNoDialog("View cells", "Continue?")) {
        break
    }
    i++
}

QPEx.clearDetections()

