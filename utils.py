'''
Utility python functions for pathology analysis with connection to PostgreSQL
'''
import os
import os.path as op
import pandas as pd
from pgclasses import *
from time import gmtime, strftime

def add_tables_to_sql(path):
    List_of_data = []
    for roots, dirs, files in os.walk(path):
        for f in files:
            if f.endswith('_all.txt'):
                List_of_data.append((roots, f))

    for r, f in List_of_data:
        ct = rawtable(op.join(r, f))
        try:
            ct.add_cell_postgresql()
        except:
            print("{}: {} commitment failed".format(strftime("%Y-%m-%d %H:%M:%S", gmtime()), f))
    try:
        ct.close_connection()
    except:
        pass

def generate_number_of_neighbour(path, save_path, ngh_range=(10,101,10)):
    List_of_data = []

    for roots, dirs, files in os.walk(path):
        for f in files:
            if f.endswith('_all.txt'):
                List_of_data.append((roots, f))

    for r, f in List_of_data:
        ct = rawtable(op.join(r, f))
        df = ct.calculate_neighbours(ngh_range)
        df.to_csv(op.join(save_path, '{}_number_neighbours.csv'.format(ct.sid)),
            index=False)
    try:
        ct.close_connection()
    except:
        pass

def generate_neighbour_features(path, save_path, max_neighbour=5, radius_list=[10, 20]):
    List_of_data = []

    for roots, dirs, files in os.walk(path):
        for f in files:
            if f.endswith('_all.txt'):
                List_of_data.append((roots, f))

    for r, f in List_of_data:
        ct = rawtable(op.join(r, f))
        nd = ct.calculate_neighbour_features(max_neighbour, radius_list)
        for nns, dfs in nd.items():
            dfs.to_csv(op.join(save_path, '{}_{}_neighbour_features.csv'.format(
                ct.sid, nns)), index=False)
    try:
        ct.close_connection()
    except:
        pass

def add_cortical_ribbons(path):
    List_of_data = []

    for roots, dirs, files in os.walk(path):
        for f in files:
            if f.endswith('_GM_anns.json'):
                List_of_data.append((roots, f))

    for r, f in List_of_data:
        ct = regiontable.cortical_ribbons(op.join(r, f))
        ct.add_annote()

    try:
        ct.close_connection()
    except:
        pass

def add_white_matter(path):
    List_of_data = []

    for roots, dirs, files in os.walk(path):
        for f in files:
            if f.endswith('_WM_anns.json'):
                List_of_data.append((roots, f))

    for r, f in List_of_data:
        ct = regiontable.white_matter(op.join(r, f))
        ct.add_annote()
    
    try:
        ct.close_connection()
    except:
        pass

def drop_slice_cell_record(sid):
    s = sqltable(sid)
    s.drop_cell_records()
    try:
        s.close_connection()
    except:
        pass

def retrieve_cell_details(sid):
    s = sqltable(sid)
    df = s.select_cell_records()
    try:
        s.close_connection()
    except:
        pass
    return df

def gnenerate_ribbon_cells(sid, save_path):
    s = sqltable(sid)
    df = s.extract_ribbon_cells()
    df.to_csv(op.join(save_path, 'ribbon_cells_{}.csv'.format(sid)), index=False)
    try:
        s.close_connection()
    except:
        pass

def generate_all_positive_cell(save_path):
    l = sqltable.get_list_of_slices()
    L = [] # list of results
    for sid in l:
        s = sqltable(sid)
        L.append(s.extract_positive_cell_counts())
    pd.concat(L).to_csv(op.join(save_path, 'positive_cells.csv'), index=False)
    try:
        s.close_connection()
    except:
        pass

def generate_all_positive_cell_threshold(save_path, threshold=0.20):
    l = sqltable.get_list_of_slices()
    L = [] # list of results
    for sid in l:
        s = sqltable(sid)
        L.append(s.extract_positive_cell_counts_threshold(threshold))
    pd.concat(L).to_csv(op.join(save_path, 'positive_cells_{:.2f}.csv'.format(threshold)), index=False)
    try:
        s.close_connection()
    except:
        pass

def generate_all_ribbon_distance(save_path):
    l = sqltable.get_list_of_slices()
    L = [] # list of results
    for sid in l:
        s = sqltable(sid)
        df = s.cortical_ribbon_distance()
        df.to_csv(op.join(save_path, 'cortical_layer_{}.csv'.format(sid)), index=False)
    try:
        s.close_connection()
    except:
        pass

