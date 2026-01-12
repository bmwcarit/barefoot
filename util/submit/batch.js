const fs = require('fs');
const path = require('path');
const net = require('net');

const args = process.argv.slice(2);

let host, port, file, id, zone = '+0000', format = 'geojson';

for (let i = 0; i < args.length; i++) {
  switch (args[i]) {
    case '--host':
      host = args[++i];
      break;
    case '--port':
      port = parseInt(args[++i]);
      break;
    case '--file':
      file = args[++i];
      break;
    case '--id':
      id = args[++i];
      break;
    case '--zone':
      zone = args[++i];
      break;
    case '--format':
      format = args[++i];
      break;
  }
}

if (!file || !host || !port) {
  console.error('Missing required arguments. Usage: node script.js --host <host> --port <port> --file <file>');
  process.exit(1);
}

if (!['geojson', 'slimjson', 'debug'].includes(format)) {
  console.error('Invalid format. Supported formats: geojson, slimjson, debug');
  process.exit(1);
}

const samples = JSON.parse(fs.readFileSync(file, 'utf8'));

for (const sample of samples) {
  if (id) {
    sample.id = id;
  }

  const tmp = `batch-${Math.floor(Math.random() * Number.MAX_SAFE_INTEGER)}`;
  const payload = JSON.stringify({ format, request: samples });

  fs.writeFileSync(tmp, payload + '\n');

  const client = net.connect({ host, port }, () => {
    const fileStream = fs.createReadStream(tmp);
    fileStream.pipe(client);
    fileStream.on('end', () => {
      client.end();
    });
  });

  let output = '';
  client.on('data', (data) => {
    output += data.toString();
    if (output.length < 16) {
      process.stdout.write(data);
    }
  });

  client.on('end', () => {
    fs.unlinkSync(tmp);
    if (!output.startsWith('SUCCESS\n')) {
      process.exit(1);
    }
  });
}
