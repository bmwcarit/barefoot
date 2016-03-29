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

__author__ = "sebastian.mattheis@bmw-carit.de"
__copyright__ = "Copyright 2015 BMW Car IT GmbH"
__license__ = "Apache-2.0"

import optparse
import getpass
import bfmap

parser = optparse.OptionParser("ways2bfmap.py [options]")
parser.add_option(
    "--source-host", dest="source_host", help="Hostname of the source database.")
parser.add_option(
    "--source-port", dest="source_port", help="Port of the source database.")
parser.add_option("--source-database", dest="source_database",
                  help="Name of the source database.")
parser.add_option(
    "--source-table", dest="source_table", help="Name of the source table.")
parser.add_option(
    "--source-user", dest="source_user", help="User of source database.")
parser.add_option("--source-password", dest="source_password",
                  help="User password of source database.")
parser.add_option(
    "--target-host", dest="target_host", help="Hostname of the target database.")
parser.add_option(
    "--target-port", dest="target_port", help="Port of the target database.")
parser.add_option("--target-database", dest="target_database",
                  help="Name of the target database.")
parser.add_option(
    "--target-table", dest="target_table", help="Name of the target table.")
parser.add_option(
    "--target-user", dest="target_user", help="User of target database.")
parser.add_option("--target-password", dest="target_password",
                  help="User password of target database.")
parser.add_option("--config", dest="config",
                  help="Configuration file for OSM data interpretation. (XML)")
parser.add_option("--append", action="store_true",
                  default=False, help="Append data if target table exists.")
parser.add_option("--printonly", action="store_true",
                  default=False, help="Do not execute commands, but print it.")

(options, args) = parser.parse_args()

if options.source_host == None or \
        options.source_port == None or \
        options.source_database == None or \
        options.source_table == None or \
        options.source_user == None or \
        options.target_host == None or \
        options.target_port == None or \
        options.target_database == None or \
        options.target_table == None or \
        options.target_user == None or \
        options.config == None:
    parser.print_help()
    exit(1)

if options.source_password == None:
    source_password = getpass.getpass("Password (source database):")
else:
    source_password = options.source_password

if options.target_password == None:
    target_password = getpass.getpass("Password (target database):")
else:
    target_password = options.target_password

config = bfmap.config(options.config)
print("Configuration imported.")

if not bfmap.exists(options.target_host, options.target_port, options.target_database,
                    options.target_table, options.target_user, target_password):
    print("Table '%s' does not exist in database '%s'." % 
          (options.target_table, options.target_database))
    bfmap.schema(options.target_host, options.target_port, options.target_database,
                options.target_table, options.target_user, target_password, options.printonly)
    print("Table '%s' has been created." % options.target_table)
else:
    print("Table '%s' already exists in database '%s'." % 
          (options.target_table, options.target_database))
    if not options.append:
        while True:
            value = raw_input(
                            "Do you want to remove table '%s' (y/n)?: " % options.target_table).lower()
            if value == 'n':
                print("Append data to table '%s'." % options.target_table)
                break
            elif value == 'y':
                bfmap.remove(options.target_host, options.target_port, options.target_database,
                            options.target_table, options.target_user, target_password, options.printonly)
                print("Table '%s' has been removed." % options.target_table)
                bfmap.schema(options.target_host, options.target_port, options.target_database,
                            options.target_table, options.target_user, target_password, options.printonly)
                print("Table '%s' has been recreated." % options.target_table)
                break

print("Inserting data ...")
bfmap.ways2bfmap(options.source_host, options.source_port, options.source_database,
                 options.source_table, options.source_user, source_password,
                 options.target_host, options.target_port, options.target_database,
                 options.target_table, options.target_user, target_password, config,
                 options.printonly)
print("Done.")
