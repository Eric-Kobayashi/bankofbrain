import qupath.lib.gui.scripting.QPEx
import qupath.lib.images.servers.ImageServer
import qupath.lib.scripting.QP
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane
import qupath.lib.regions.RegionRequest

import javax.imageio.ImageIO

img = QP.getCurrentServer().getAssociatedImage("Series 1 (label image)")
def imagename = QP.getCurrentServer().getPath().split('/').last().replace('.svs', '').replace('[--series, 0]', '')

def pathOutput = "C:/Users/Eric/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/fifth_run/labels_image_project/thumbnails"
def fileOutput = new File(pathOutput, imagename + '_label.png')
ImageIO.write(img, 'PNG', fileOutput)
