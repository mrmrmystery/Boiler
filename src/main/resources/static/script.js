var video = document.createElement('video');
video.setAttribute('playsinline', '');
video.setAttribute('autoplay', '');
video.setAttribute('muted', '');
video.style.width = '1920px';
video.style.height = '1080px';
video.style.display = 'none';
document.body.appendChild(video);

var prevvideo = document.getElementById('preview');
prevvideo.setAttribute('playsinline', '');
prevvideo.setAttribute('autoplay', '');
prevvideo.setAttribute('muted', '');
document.body.appendChild(prevvideo);

var facingMode = "user"; // Can be 'user' or 'environment' to access back or front camera (NEAT!)
var constraints = {
  audio: false,
  video: {
   facingMode: facingMode
  }
};

navigator.mediaDevices.getUserMedia(constraints).then(function success(stream) {
  video.srcObject = stream;
  prevvideo.srcObject = stream;
});

let button = document.getElementById("startstop");
button.addEventListener("click", () => {
    if(button.innerHTML == "start") {
        button.innerHTML = "stop";
        var id = document.getElementById("id").value;
        setupWs(id);
    } else {
        button.innerHTML = "start";
        ws.close();
    }
});

let isReady = false;
let ws;

function setupWs(id) {
    if(ws !== undefined) ws.close();
    let prefix = "ws";
    if(window.location.protocol === "https:") prefix = "wss";

    ws = new WebSocket(`${prefix}://${window.location.host}/ws?id=${id}`);
    ws.onopen = function() {
        console.log("Connected!");
        isReady = true;
    }

    ws.onclose = function() {
        console.log("Disconnected!");
        isReady = false;
    }

    ws.onerror = function() {
        console.log("Error!");

    }
}

let Iwidth = 1920;
let Iheight = 1080;
let Iquality = 0.85;

setInterval(() => {
    Iwidth = document.getElementById("i_w").value;
    Iheight = document.getElementById("i_h").value;
    Iquality = document.getElementById("i_q").value;
    render();
}, 50);

function render() {
    var canvas = document.createElement('canvas');
    canvas.width = Iwidth;
    canvas.height = Iheight;
    var ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, Iwidth, Iheight);
    canvas.toBlob((blob) => {
        if(isReady) {
            ws.send(blob);
        }
    }, 'image/jpeg', Iquality);
}

async function setupMicrophoneWorklet() {
    try {
        await audioContext.audioWorklet.addModule('microphone-worklet.js');
        const microphoneWorkletNode = new AudioWorkletNode(audioContext, 'microphone-worklet');



        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const microphoneSource = audioContext.createMediaStreamSource(stream);
        microphoneSource.connect(microphoneWorkletNode);
        microphoneWorkletNode.connect(audioContext.destination);

        console.log('Mikrofonzugriff aktiviert!');
    } catch (err) {
        console.error('Fehler beim Einrichten des AudioWorklets:', err);
    }
}

// AudioContext erstellen
const audioContext = new (window.AudioContext || window.webkitAudioContext)();
let currentTime = audioContext.currentTime;

// Starte die Audiobearbeitung
audioContext.suspend();

// Benutzerinteraktion erforderlich, um den AudioContext zu starten
document.addEventListener('click', async () => {
    await audioContext.resume();
    currentTime = audioContext.currentTime;
    await setupMicrophoneWorklet();
});
