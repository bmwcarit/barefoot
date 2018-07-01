#!/usr/bin/env python

#
# Copyright (C) 2015, BMW Car IT GmbH
#
# Author: Sebastian Mattheis <sebastian.mattheis@bmw-carit.de>
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
# in compliance with the License. You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
# writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
# language governing permissions and limitations under the License.
#
from __future__ import print_function, division, absolute_import

__author__ = "sebastian.mattheis@bmw-carit.de"
__copyright__ = "Copyright 2015 BMW Car IT GmbH"
__license__ = "Apache-2.0"

import psycopg2


class Database(object):
    def __init__(self, host, port, database, user, password, printonly):
        self.host = host
        self.port = port
        self.database = database
        self.user = user
        self.password = password
        self.printonly = printonly
        self.connection, self.cursor = self.open()

    def open(self):
        try:
            dbcon = psycopg2.connect(
                host=self.host,
                port=self.port,
                database=self.database,
                user=self.user,
                password=self.password
            )
            cursor = dbcon.cursor()
        except:
            print("Connection to database failed.")
            raise
        return dbcon, cursor

    def close(self):
        self.cursor.close()
        self.connection.close()

    def execute(self, query, params=(), commit=True):
        if self.printonly:
            print(query % params)
            return
        try:
            self.cursor.execute(query, params)
            if commit:
                self.connection.commit()
        except:
            print("Database transaction failed, query: {}.".format(query))
            raise

    def exists(self, table):
        """Check whether the table already exists in the database."""
        query = """SELECT COUNT(tablename) FROM pg_tables WHERE 
                schemaname='public' AND tablename=%s;"""
        self.execute(query, params=(table, ))
        if self.printonly:
            return True
        if self.cursor.fetchone()[0] == 0:
            return False
        else:
            return True

    def drop(self, table):
        """Remove a table from the database."""
        print('Dropping table {}.'.format(table))
        query = "DROP TABLE {};".format(table)
        self.execute(query)

    def drop_if_exists(self, table):
        """Drop the table if it exists."""
        if self.exists(table):
            print('Table {} already exists, dropping it.'.format(table))
            self.drop(table)


def get_confirmation_for_table_deletion(table, database):
    """Ask user to confirm to delete the table."""
    while True:
        print("Table '{}' already exists in database '{}'."
              .format(table, database))
        msg = "Do you want to remove the table (y/n)? [n]: "
        try:
            value = raw_input(msg)
        except NameError:
            value = input(msg)
        if value.lower() == 'y':
            return True
        elif value.lower() == 'n' or value == '':
            return False
