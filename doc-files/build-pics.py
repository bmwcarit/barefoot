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

import os

for root, dirs, files in os.walk(".", topdown=False):
	for name in files:
		if name.endswith('.tex'):
			pic = os.path.splitext(name)[0]
			os.system("pdflatex -output-directory %s %s" % (root,os.path.join(root, pic + '.tex')))
			os.system("pdfcrop %s %s" % (os.path.join(root, pic + '.pdf'), os.path.join(root, pic + '.pdf')))
			os.system("convert -quality 100 -density 300 %s %s" % (os.path.join(root, pic + '.pdf'), os.path.join(root, pic + '.png')))
