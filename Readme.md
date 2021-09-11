# bankofbrain - Package Manual
Eric Hidari (<feedbackeric@gmail.com>) 2021-09-11

## Analysis SOP
This section describes the Standard Operation Procedure (SOP) for the analysis of IHC (DAB-AT8 tau) post mortem brain images. The goal is to generate cellular level structured data of quantified pathology and morphology information. Groovy scripts (within `Qupath_script` folder) and Python functions (within `util.py`) mentioned in this document are loaded from `bankofbrain`. Commands and plugins in __QuPath__ are highlighted.

### Segmentation
1. Import the '.svs' images into __QuPath 0.2.3__ (__StarDist__ plugin needs to be installed, see [QuPath website](https://qupath.readthedocs.io/en/stable/docs/advanced/stardist.html)) and create a project. Open __Script Editor__ and load `Tissue_detection.groovy`, adjust the threshold to separate the tissue block from the background. Change the threshold until the entire tissue is selected.
2. Manually inspect the annotation to specify the boundary. Set the annotation to mutable and remove large blood vessels, black fractal artefacts, black fiducial markers, blurred tissue and overlapped tissue. 
3. Segment the cortical region blocks to white and grey matter. Use __Brush tool__ to outline the grey matter at low magnification and adjust the boundary at high magnification. Look up the image at high contrast can guide one through the drawings. Outline the white matter by annotation subtraction. Assign the white matter annotation with the QuPath built-in class _'Tumor'_ and grey matter with _'Stroma'_. Select a cortical ribbon within the cortical grey matter that has a relatively uniform thickness, and assign it with _'Region'_.
4. For cerebellum sections, first identify the granular cell layer using the __Pixel classifier__ plugin with the pre-trained classifier `granular_cells.json`. Then use __Brush tool__ to select the white matter. Assign the white matter annotation with _'Tumor'_ and granular cells with _'Immune cells'_.
5. For basal ganglia sections, a neuropathologist will be required to conduct the segmentation of globus pallidus, striatum and subthalamic nucleus. 

### Detection
1. Run `Stardist_segment.groovy` within QuPath on the entire project for cell detection and measurements. Run `Stardist_segment_BG.groovy` on basal ganglia slides annotated by the neuropathologist. Parameters related to Stardist and cell expansion can be adjusted in the script.
2. Run `Export_cortical_ribbons.groovy` and `Export_white_matter.groovy` to output the cortical ribbon and the white matter annotations to json files.

### Serialisation
1. Modify `sql_config.json` file to configure the PostgreSQL database for storing the information.
2. Run `add_tables_to_sql` function to add cell characterisation tables to the database, including a BRIN index table for fast queries.
3. Run `add_cortical_ribbons` and `add_white_matter` functions to add the cortical ribbon and the white matter annotations to the database.

### Analysis
1. Run `generate_all_positive_cell` function to retrieve the number of tau positive/negative cells for each slide subregions. Alternatively, run `generate_all_positive_cell_threshold` function if a custom threshold for dividing positive/negative cells is desired. 
2. Run `generate_all_ribbon_distance` function to measure distances to the grey/white matter boundary for each cortical ribbon cells for cortical layer distribution analysis.
3. Run the statistical analysis and figure plotting (detailed in the PSP cohort paper) using the workflow defined in the Jupyter Notebook `Histology-pathology-PSP-cohort-paper.ipynb`.


## Scripts documentation
This sections document the scripts within the `bankofbrain` package.

### QuPath Groovy scripts
These scripts are located in the `Qupath_script` folder and should be run within the QuPath __Script Editor__.

#### `Export_all_annotations.groovy`
Export all annotations of the image to a json file. Useful for transferring annotations between projects.

#### `Export_label_images.groovy`
Export the label images of the slide for easy access of the metadata.

#### `Import_cortical_ribbon.groovy`
Import the annotation of cortical ribbon into the opened whole-slide image. Can be modified to import other annotations.

#### `Import_and_calc_Jaccard.groovy`
Import an annotation and calculate the Jaccard index between the imported region and the current region. Useful for assessing interrater segmentation agreement.

#### `	Save_thumbnail.groovy`
Make a screenshot of the section and save it as a thumbnail image.

#### `Export_cell_images.groovy`
For each detected cell in the selected region, export a small square field of view image. Useful for generating images for machine learning training.

#### `Assign_cell_classes.groovy`
Assign the cell type from data table to the detected cells in the image and visualise them. Useful for visualising cell type distribution in the image after machine learning classification.

#### `Export_manual_cell_annotation.groovy`
Export the cell annotations to json file for cross-validation with automatic cell detection.

#### `Measure_tau_threshold.groovy`
Measure the pixel-based tau stain in all subregions and all detected cells, with the threshold configured in `tau.json`. Useful for extracting the proportion of somatic tau within the total cell burden.

#### `Export_annotation_measurement.groovy`
Export the measurement table of all annotations of the image.

#### `Move_viewer.groovy`
Move the viewer to FOVs centring around cells from the data table. Useful for viewing selected cells of interest following classification, such as checking scenarios where cells are misclassified.

#### `Tissue_detection.groovy`
Automatically detect the tissue boundary with the defined threshold. Adjust the threshold so that the correct region is selected.

#### `Stardist_segment.groovy`
Detect and characterise the cells in the whole slide image using StarDist (The pre-trained deep learning model `he_heavy_augment` is used, download [here](https://github.com/stardist/stardist-imagej/tree/master/src/main/resources/models/2D)) and assign them to the region where they are located. If the region is annotated basal ganglia, please use `Stardist_segment_BG.groovy` instead.

#### `Stardist_segment_BG.groovy`
See above.


### Python utility functions
These functions are located at `utils.py` and could be run inside the folder where the dependent class library `pgclasses.py` is located.

#### `add_tables_to_sql`
Add cell data to the PostgreSQL database and table specified in the configuration file `sql_config.json`.

#### `generate_number_of_neighbour`
Generate the number of neighbour features for each cell in the data table. The definition of neighbour radiuses range can be modified as a parameter.

#### `generate_neighbour_features`
Generate the mean value of features for the neighbourhood of each cell in the data table. The definition of number neighbour and radius neighbour can be modified as parameters.

#### `add_cortical_ribbons`, `add_white_matter`
Add cortical ribbon or white matter annotation to the PostgreSQL database.

#### `drop_slide_cell_record`
Drop the cell record of the slide from the PostgreSQL cell table.

#### `retrieve_cell_details`
Retrieve the cell record of the slide from the PostgreSQL cell table.

#### `generate_ribbon_cells`
Generate a table of all cells located at the cortical ribbon for the slide. Useful for extracting cells from the grey matter of the brain with a defined layered structure.

#### `generate_all_positive_cell`
Generate a table counting the tau positive and negative cells for all subregions of all slides using the default colour threshold.

#### `generate_all_positive_cell_threshold`
Generate a table counting the tau positive and negative cells for all subregions of all slides using a custom colour threshold defined as a parameter.

#### `generate_all_ribbon_distance`
Generate a table of all cells located at the cortical ribbon for the slide with their distance to the white/grey matter boundary measured. Useful for analysing the cortical layer distribution of tau pathology.
