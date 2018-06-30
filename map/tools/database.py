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
        self.db_connection, self.cursor = self.open_connection()

    def open_connection(self):
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

    def close_connection(self):
        self.cursor.close()
        self.db_connection.close()

    def do_query(self, query, params=()):
        try:
            if self.printonly:
                print(query % params)
            else:
                self.cursor.execute(query, params)
                self.db_connection.commit()
        except:
            print("Database transaction failed, query: {}.".format(query))
            raise


