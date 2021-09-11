# -*- coding: utf-8 -*-
'''
Python library classes for SQL operations
'''

import numpy as np
from numpy import dtype
import pandas as pd
from pandas import DataFrame as DF
import os
import os.path as op
import json
import psycopg2
from time import localtime, strftime
import math

class rawtable():
    '''
    Class to manage raw cell datatables generated from StarDist/QuPath workflow
    '''
    with open('sql_config.json', r) as config:
        sqlconfig = json.load(config)
    conn = _connect()  # create a class-wise connection to the database

    def __init__(self, path):
        '''
        Initiate a class instance with the path to cell datatable
        '''
        self.path = path
        self.sid = 'S{}'.format(op.basename(self.path).split('_')[0])
        self.threshold = .15 # the threshold of DAB cytoplasm mean value for tau positive cells
        self.cols_to_remove = ['Image','Parent','ROI']
        self.df = self._add_class(pd.read_csv(self.path, sep='\t', encoding="ISO-8859-1")
                    .rename(columns=rawtable._fix_column_name))
        self.df['slice_ID'] = self.sid
        self.table_name = self.sqlconfig['table_name']

    @staticmethod
    def _fix_column_name(s):
        '''
        Transform special characters to underscore for easy python reading
        '''
        sfixed = s
        to_fix = [': ',':',' ', '/', '^', '.']
        for f in to_fix:
            if f in s:
                sfixed = sfixed.replace(f, '_')
        sfixed = ''.join([i if ord(i) < 128 else 'u' for i in sfixed])
        sfixed = sfixed.replace('uum', 'um')
        return sfixed

    @classmethod
    def _connect(cls):
        '''
        Connect to SQL database
        '''
        try:
            conn = psycopg2.connect(cls.sqlconfig['SQL_connect'])
        except:
            print('Unable to connect to the database')
        return conn

    def _add_class(self, df):
        '''
        Calculate Class column (tau Positive or Negative) and delete
        unused columns
        '''
        df_neu = df.copy()
        df_neu['Class'] = df_neu['DAB_Cytoplasm_Mean'].map(lambda x: 'Positive' if x >= self.threshold else 'Negative')
        df_neu = df_neu.drop(columns = self.cols_to_remove)
        return df_neu

    def _create_sql_col(self):
        '''
        In case the table does not exist in the database, create the datatable
        '''
        # create the cell data table
        conn = rawtable._connect()
        with conn.cursor() as cur:
            dtype_translate = {
                dtype('O'): 'TEXT',
                dtype('float64'): 'FLOAT8',
                dtype('int64'): 'INT'
            }
            columns = ',\n'.join(['{} {}'.format(col, dtype_translate[dt]) for col, dt in self.df.dtypes.items()])
            command = "CREATE TABLE "+self.table_name+" (cell_id BIGSERIAL PRIMARY KEY,\n{}\n);".format(columns)
            try:
                cur.execute(command)
            except:
                pass
        conn.commit()
        conn.close()

        # create the index table
        conn = rawtable._connect()
        with conn.cursor() as cur:
            command = "CREATE TABLE "+self.table_name+"_slice_ix (start_id BIGINT,\n end_id BIGINT,\n, slice_id TEXT\n)"
            try:
                cur.execute(command)
            except:
                pass
        conn.commit()
        conn.close()

    def _add_values(self):
        '''
        Add the datatable into SQL database and output indexes for the index table
        '''
        def value_fix(v):
            if isinstance(v, str): return "\'{}\'".format(v)
            elif np.isnan(v): return 'NULL'
            else: return str(v)

        retrieve_id_command = '''SELECT MAX(cell_id) FROM {};'''.format(self.table_name)
        with self.conn.cursor() as cur:
            cur.execute(retrieve_id_command)
            self.start_id = cur.fetchall()[0][0] + 1 # start cell_id

        with self.conn.cursor() as cur:
            for i, data in df.iterrows():
                command = 'INSERT INTO {}'.format(self.table_name) +' ({})\nVALUES ({});'.format(','.join(data.index), ','.join(map(value_fix, data.values)))
                cur.execute(command)

        with self.conn.cursor() as cur:
            cur.execute(retrieve_id_command)
            self.end_id = cur.fetchall()[0][0] # end cell_id
        self.conn.commit()

    def _add_index(self):
        '''
        Add the BRIN index into the database
        '''
        command = 'INSERT INTO {}_slice_ix'.format(self.table_name)+' (start_id, end_id, slice_id)\nVALUES (%s,%s,%s)'
        with self.conn.cursor() as cur:
            cur.execute(command, (self.start_id, self.end_id, self.sid))

    def add_cell_postgresql(self):
        '''
        Add the cell datatable to the postgresSQL database, and add BRIN index 
        '''
        # First check if the SQL tables have been created, and create them if not
        try:
            assert rawtable.TABLE_EXIST
        except:
            self._create_sql_col()
        finally:
            rawtable.TABLE_EXIST = True

        # add individual records into the table
        self._add_values()
        self._add_index()
        print("{}: {} committed".format(strftime("%Y-%m-%d %H:%M:%S", localtime()), self.sid))

    def close_connection(self):
        '''
        Close the connection to postgresql database if one exists
        '''
        try:
            self.conn.close()
        except:
            pass

    def calculate_neighbours(self, ngh_range=range(10,101,10)):
        '''
        To calculate the number of neighbours for each cell in the datatable
        '''
        from sklearn.neighbors import NearestNeighbors
        from collections import defaultdict

        def num_neighbours(df):
            xy = df[['X', 'Y']].values
            rip = defaultdict(list)
            for r in ngh_range:
                mdl = NearestNeighbors(radius=r, algorithm='auto').fit(xy)
                neigh = mdl.radius_neighbors(return_distance=False)
                e_arr = np.array(list(map(len, neigh)))
                rip['NN_{}_um'.format(r)] = e_arr
            return df[['X', 'Y']].join(DF(rip)).assign(slice_id=self.sid)

        if len(self.df) >= 1:
            df = self.df.rename(columns = {'Centroid_X_um':'X', 'Centroid_Y_um':'Y'})
            return num_neighbours(df)
        else:
            return None


    def calculate_neighbour_features(self, max_neighbour = 5, radius_list = [10, 20]):
        '''
        Generate the smoothed feature of each cell (mean value of its neighbours)
        '''
        from sklearn.neighbors import NearestNeighbors

        features = ['Detection_probability', 'Nucleus_Area_um_2', 'Nucleus_Length_um',
           'Nucleus_Circularity', 'Nucleus_Solidity', 'Nucleus_Max_diameter_um',
           'Nucleus_Min_diameter_um', 'Cell_Area_um_2', 'Cell_Length_um',
           'Cell_Circularity', 'Cell_Solidity', 'Cell_Max_diameter_um',
           'Cell_Min_diameter_um', 'Nucleus_Cell_area_ratio',
           'Hematoxylin_Nucleus_Mean', 'Hematoxylin_Nucleus_Median',
           'Hematoxylin_Nucleus_Min', 'Hematoxylin_Nucleus_Max',
           'Hematoxylin_Nucleus_Std_Dev_', 'Hematoxylin_Cytoplasm_Mean',
           'Hematoxylin_Cytoplasm_Median', 'Hematoxylin_Cytoplasm_Min',
           'Hematoxylin_Cytoplasm_Max', 'Hematoxylin_Cytoplasm_Std_Dev_',
           'Hematoxylin_Membrane_Mean', 'Hematoxylin_Membrane_Median',
           'Hematoxylin_Membrane_Min', 'Hematoxylin_Membrane_Max',
           'Hematoxylin_Membrane_Std_Dev_', 'Hematoxylin_Cell_Mean',
           'Hematoxylin_Cell_Median', 'Hematoxylin_Cell_Min',
           'Hematoxylin_Cell_Max', 'Hematoxylin_Cell_Std_Dev_', 'DAB_Nucleus_Mean',
           'DAB_Nucleus_Median', 'DAB_Nucleus_Min', 'DAB_Nucleus_Max',
           'DAB_Nucleus_Std_Dev_', 'DAB_Cytoplasm_Mean', 'DAB_Cytoplasm_Median',
           'DAB_Cytoplasm_Min', 'DAB_Cytoplasm_Max', 'DAB_Cytoplasm_Std_Dev_',
           'DAB_Membrane_Mean', 'DAB_Membrane_Median', 'DAB_Membrane_Min',
           'DAB_Membrane_Max', 'DAB_Membrane_Std_Dev_', 'DAB_Cell_Mean',
           'DAB_Cell_Median', 'DAB_Cell_Min', 'DAB_Cell_Max', 'DAB_Cell_Std_Dev_']

        def neighbour_features(df):
            xy = df[['X', 'Y']].values
            all_neighbouring_features = {} # save them in sepearte files from dictionary
            for r in radius_list: 
                mdl = NearestNeighbors(radius=r, algorithm='auto').fit(xy) # fixed distance
                neigh = mdl.radius_neighbors(return_distance=False)
                neighbours = []
                for cell in neigh:
                    neighbours.append(df.loc[cell, features].mean())
                nn = 'NN_{}um'.format(r)
                all_neighbouring_features[nn] = (df[['X', 'Y']].join(DF(neighbours)
                    .rename(columns={col: '{}_{}'.format(col, nn) for col in features}))
                    .assign(slice_id=self.sid))

            mdl = NearestNeighbors(n_neighbors=max_neighbour, algorithm='auto').fit(xy) # fixed number of neighbours
            dist, neigh = mdl.kneighbors(return_distance=True)
            for n in range(1, max_neighbour+1): 
                neighbours = []
                for cell in neigh:
                    neighbours.append(df.loc[cell[:n], features].mean())
                nn = 'NN_{}cells'.format(n)
                all_neighbouring_features[nn] = (df[['X', 'Y']].join(DF(neighbours)
                    .rename(columns={col: '{}_{}'.format(col, nn) for col in features}))
                    .assign(slice_id=self.sid))

            return all_neighbouring_features

        if len(self.df) >= 1:
            df = self.df.rename(columns = {'Centroid_X_um':'X', 'Centroid_Y_um':'Y'})
            return neighbour_features(df)
        else:
            return None

class regiontable():
    '''
    Class for adding annotated anatomical regions into the SQL database
    '''
    with open('sql_config.json', r) as config:
        sqlconfig = json.load(config)
    conn = _connect()  # create a class-wise connection to the database
    TABLE_EXIST = {} # control whether an SQL table has been created

    def __init__(self, path, table):
        self.path = path
        self.table = table
        self.geom = _read_json(self.path)
        self.table_name = '{}_{}'.format(self.sqlconfig['table_name'], self.table)

    @classmethod
    def _connect(cls):
        '''
        Connect to SQL database
        '''
        try:
            conn = psycopg2.connect(cls.sqlconfig['SQL_connect'])
        except:
            print('Unable to connect to the database')
        return conn

    @classmethod
    def cortical_ribbons(cls, path):
        return cls(path, 'cortical_ribbons')

    @classmethod
    def white_matter(cls, path):
        return cls(path, 'white_matter')

    def _create_table(self):
        conn = regiontable._connect()
        with conn.cursor() as cur:
            command = "CREATE TABLE {} (region_id BIGSERIAL PRIMARY KEY, slice_ID TEXT, boundary GEOMETRY);".format(self.table_name)
            try:
                cur.execute(command)
                conn.commit()
            except:
                pass
        conn.close()

    def _read_json(self):
        sid = 'S{}'.format(op.basename(self.path).split('_')[0])
        with open(self.path, 'r') as ann:
            roi = json.load(ann)
        return roi

    def add_annote(self):
        try:
            assert rawtable.TABLE_EXIST[self.table_name]
        except:
            self._create_sql_col()
        finally:
            rawtable.TABLE_EXIST[self.table_name] = True

        with self.conn.cursor() as cur:
            command = "INSERT INTO {} (slice_ID, boundary)\nVALUES (%s, ST_SetSRID(ST_GeomFromGeoJSON(%s), 0));".format(self.table_name)
            cur.execute(command, (self.sid, self.geom))
        self.conn.commit()

    def close_connection(self):
        '''
        Close the connection to postgresql database if one exists
        '''
        try:
            self.conn.close()
        except:
            pass


class sqltable():
    '''
    Class for conducting SQL operations for celltable data analysis
    '''
    with open('sql_config.json', r) as config:
        sqlconfig = json.load(config)
    conn = _connect()  # create a class-wise connection to the database

    def __init__(self, sid):
        self.sid = sid
        self.table_name = self.sqlconfig['table_name']

    @classmethod
    def _connect(cls):
        '''
        Connect to SQL database
        '''
        try:
            conn = psycopg2.connect(cls.sqlconfig['SQL_connect'])
        except:
            print('Unable to connect to the database')
        return conn

    @classmethod
    def get_list_of_slices(cls):
        with self.conn.cursor() as cur:
            cur.execute("SELECT slice_id FROM {}_slice_ix".format(cls.sqlconfig['table_name']))
            l = [s.replace('\'', '') for s in sum(cur.fetchall(), ())]
        return l

    def select_cell_records(self):
        command = '''SELECT *
        FROM {0} WHERE cell_id BETWEEN (SELECT start_id FROM {0}_slice_ix WHERE slice_id = %(sid)s) AND
        (SELECT end_id FROM {0}_slice_ix WHERE slice_id = %(sid)s)'''.format(self.table_name)
        return pd.read_sql(command, self.conn, params={'sid':self.sid})

    def drop_cell_records(self):
        command = '''DELETE
        FROM {0} WHERE cell_id BETWEEN (SELECT start_id FROM {0}_slice_ix WHERE slice_id = %s) AND
        (SELECT end_id FROM {0}_slice_ix WHERE slice_id = %s)
        '''.format(self.table_name)
        try:
            with self.conn.cursor() as cur:
                cur.execute(command, (self.sid, self.sid))
        except:
            pass

        command = '''DELETE
        FROM {}_slice_ix WHERE slice_id = %s
        '''.format(self.table_name)
        try:
            with self.conn.cursor() as cur:
                cur.execute(command, (self.sid, ))
            self.conn.commit()
        except:
            print('No records to drop for {}'.format(self.sid))

    def drop_region_records(self, table):
        command = '''DELETE
        FROM {}_%s WHERE slice_id = %s
        '''.format(self.table_name)

        try:
            with self.conn.cursor() as cur:
                cur.execute(command, (table, self.sid))
            self.conn.commit()
        except:
            print('No records to drop for {} in {} table'.format(self.sid, table))
        
    def drop_all_records(self):
        self.drop_cell_records()
        self.drop_region_records('cortical_ribbons')
        self.drop_region_records('white_matter')

    def extract_positive_cell_counts(self):
        command = '''SELECT
       COUNT(*) FILTER ( WHERE class = 'Negative' ) Negative,
       COUNT(*) FILTER ( WHERE class = 'Positive' ) Positive,
       slice_id,
       name subregion
        FROM {0}
        WHERE ({0}.cell_id BETWEEN (SELECT start_id FROM {0}_slice_ix WHERE slice_id = %(sid)s) AND
            (SELECT end_id FROM {0}_slice_ix WHERE slice_id = %(sid)s))
        GROUP BY slice_id, name
        '''.format(self.table_name)
        try:
            return pd.read_sql(command, self.conn, params={'sid':self.sid})
        except:
            print('No records can be retrieved for {}'.format(self.sid))
            return None

    def extract_positive_cell_counts_threshold(self, threshold):
        command = '''SELECT
       COUNT(*) FILTER ( WHERE dab_cytoplasm_mean < %(ths)s) Negative,
       COUNT(*) FILTER ( WHERE dab_cytoplasm_mean >= %(ths)s) Positive,
       slice_id,
       name subregion
        FROM {0}
        WHERE ({0}.cell_id BETWEEN (SELECT start_id FROM {0}_slice_ix WHERE slice_id = %(sid)s) AND
            (SELECT end_id FROM {0}_slice_ix WHERE slice_id = %(sid)s))
        GROUP BY slice_id, name
        '''.format(self.table_name)
        try:
            return pd.read_sql(command, self.conn, params={'sid':self.sid, 'ths':threshold})
        except:
            print('No records can be retrieved for {}'.format(self.sid))
            return None

    def extract_ribbon_cells(self):
        '''
        Output table of cells that are located at the cortical ribbon
        '''
        command = '''SELECT slice_id, centroid_x_um X, centroid_y_um Y
        FROM {0}
        INNER JOIN {0}_slice_ix USING (slice_id)
        INNER JOIN {0}_cortical_ribbons USING (slice_id)
        WHERE ({0}.cell_id BETWEEN (SELECT start_id FROM {0}_slice_ix WHERE slice_id = %(sid)s) AND
        (SELECT end_id FROM {0}_slice_ix WHERE slice_id = %(sid)s)) AND
        (st_contains((SELECT boundary FROM {0}_cortical_ribbons WHERE slice_id = %(sid)s),
        ST_SetSRID(ST_MakePoint({0}.centroid_x_um/0.2528, {0}.centroid_y_um/0.2528), 0)))
        '''.format(self.table_name)
        try:
            return pd.read_sql(command, self.conn, params={'sid':self.sid})
        except:
            print('No records can be retrieved for {}'.format(self.sid))
            return None

    def cortical_ribbon_distance(self):
        command = '''SELECT class, slice_id,
       ST_Distance(ST_SetSRID(ST_MakePoint({0}.centroid_x_um/0.2528, {0}.centroid_y_um/0.2528), 0),
                   (SELECT boundary FROM {0}_white_matter WHERE slice_id = %s)) AS wm_distance
        FROM {0}
        INNER JOIN {0}_slice_ix USING (slice_id)
        INNER JOIN {0}_white_matter USING (slice_id)
        INNER JOIN {0}_cortical_ribbons USING (slice_id)
        WHERE ({0}.cell_id BETWEEN (SELECT start_id FROM {0}_slice_ix WHERE slice_id = %(sid)s) AND
        (SELECT end_id FROM {0}_slice_ix WHERE slice_id = %(sid)s)) AND
        ({0}.name = 'Grey_matter') AND
        (st_contains((SELECT boundary FROM {0}_cortical_ribbons WHERE slice_id = %(sid)s),
        ST_SetSRID(ST_MakePoint({0}.centroid_x_um/0.2528, {0}.centroid_y_um/0.2528), 0)))
        '''.format(self.table_name)
        try:
            df = pd.read_sql(command, self.conn, params={'sid':self.sid})
        except:
            print('No records can be retrieved for {}'.format(self.sid))
            return None
        if len(df) >= 1:
            cortical_thickness = np.quantile(df['wm_distance'], .99)
            df['layer'] = df['wm_distance'].map(lambda x: math.floor(x/cortical_thickness*30))
            df['layer'] = df['layer'].map(lambda x: 1 if x > 29 else 30-x)
        else:
            print('No records can be retrieved for {}'.format(self.sid))
            return None
        return df

    def close_connection(self):
        '''
        Close the connection to postgresql database if one exists
        '''
        try:
            self.conn.close()
        except:
            pass
