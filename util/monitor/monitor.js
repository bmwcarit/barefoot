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

if (process.argv.length != 4) {
	console.log("usage: monitor <host> <port>");
	process.exit(1);
}

var objects = [];
var zmq = require('zmq');
var subscriber = zmq.socket('sub');
subscriber.subscribe("");

subscriber.on('message', function(message) {
	update = JSON.parse(message);
	id = update['id'];
	
	if (id in objects && objects[id]['time'] >= update['time']) {
		console.log('warning: out of order update for object ' + id);
		return;
	}
	
	if (!('point' in update)) {
		console.log('delete object ' + id);
		delete objects[id];
	} else if (id in objects) {
		console.log('update object ' + id);
		objects[id] = update;
	} else {
		console.log('insert object ' + id);
		objects[id] = update;
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
    
    for (var id in objects) {
    	socket.emit('message', objects[id]);
    }
    
    socket.on('disconnect', function() {
        console.log('client disconnected');
    });
});

process.on('SIGINT', function() {
        console.log('shut down');
        process.exit();
});

console.log('connect and listen to tracker ...');
subscriber.connect('tcp://' + process.argv[2] + ':' + process.argv[3]);

http.listen(3000, function() {
    console.log('listening on *:3000');
});