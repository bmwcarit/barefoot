/**
 * @license
 * Copyright (C) 2016, BMW Car IT GmbH
 *
 * Author: Sebastian Mattheis <sebastian.mattheis@bmw-carit.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

if (process.argv.length != 5 && (process.argv.length != 6 ||
		(process.argv[5] != 'buffer=true' && process.argv[5] != 'buffer=false'))) {
	console.log("usage: monitor <port> <tracker-host> <tracker-port> [buffer=<true/false>, default: false]");
	process.exit(1);
}

var buffer = process.argv.length < 6 ? false : process.argv[5] == 'buffer=true';
var objects = [];
var zmq = require('zmq');
var subscriber = zmq.socket('sub');
subscriber.subscribe("");

subscriber.on('message', function(message) {
	update = JSON.parse(message);
	id = update['id'];
	
	if (buffer && id in objects && objects[id]['time'] >= update['time']) {
		console.log('warning: out of order update for object ' + id);
		return;
	}

	if (!('point' in update)) {
		if (buffer) {
			console.log('delete object ' + id);
			delete objects[id];
		}
	} else if (id in objects) {
		if (buffer) {
			objects[id] = update;
		}
	} else {
		if (buffer) {
			console.log('insert object ' + id);
			objects[id] = update;
		}
	}
	
	io.emit('message', update);
});

var path = require('path')
var express = require('express')
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);
 
app.use(express.static(path.join(__dirname, './public')));

app.get('/', function(req, res) {
    res.sendfile('index.html');
});

app.get('/messages', function(req, res) {
    res.sendfile('index.html');
});

io.on('connection', function(socket) {
	console.log('client connected');

	if (buffer) {
		for (var id in objects) {
			socket.emit('message', objects[id]);
		}
    }
    
    socket.on('disconnect', function() {
        console.log('client disconnected');
    });
});

process.on('SIGINT', function() {
        console.log('shut down');
        process.exit();
});

if (buffer) {
	console.log('connect and listen to tracker (buffered) ...');
} else {
	console.log('connect and listen to tracker (unbuffered) ...');
}

subscriber.connect('tcp://' + process.argv[3] + ':' + process.argv[4]);

http.listen(process.argv[2], function() {
    console.log('listening on *:' + process.argv[2]);
});
