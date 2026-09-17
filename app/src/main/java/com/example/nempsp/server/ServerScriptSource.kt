package com.example.nempsp.server

object ServerScriptSource {
    const val PYTHON_SCRIPT = """#!/usr/bin/env python3
# ==========================================================
# NEMPSP Server - Chromebook & PC Companion Server
# Receives WiFi (UDP), USB (ADB TCP 127.0.0.1) and Bluetooth
# ==========================================================
import socket
import struct
import threading
import time
import sys

# Default Port
PORT = 8989

# Try importing virtual gamepad or keyboard fallback
UINPUT_AVAILABLE = False
try:
    import uinput
    # Linux / Chromebook virtual gamepad
    events = (
        uinput.BTN_A, uinput.BTN_B, uinput.BTN_X, uinput.BTN_Y,
        uinput.BTN_TL, uinput.BTN_TR, uinput.BTN_START, uinput.BTN_SELECT,
        uinput.ABS_X + (-128, 127, 0, 0),
        uinput.ABS_Y + (-128, 127, 0, 0),
    )
    device = uinput.Device(events)
    UINPUT_AVAILABLE = True
    print("[NEMPSP] Virtual Gamepad uinput initialized!")
except Exception:
    print("[NEMPSP] Note: 'uinput' not installed. Running in Terminal / Keyboard simulation mode.")
    print("[NEMPSP] Tip on Linux/Chromebook: pip install python-uinput OR pip install pynput")

BUTTON_NAMES = [
    "UP", "RIGHT", "DOWN", "LEFT",
    "TRIANGLE", "CIRCLE", "CROSS", "SQUARE",
    "L", "R", "SELECT", "START",
    "HOME", "VOL-", "VOL+", "NOTE"
]

def parse_packet(data):
    if len(data) >= 9 and data[0] == 0x4E and data[1] == 0x4D: # Magic 'NM'
        seq = data[2] | (data[3] << 8)
        mask = data[4] | (data[5] << 8)
        ax = data[6] - 128
        ay = data[7] - 128
        pressed = [BUTTON_NAMES[i] for i in range(16) if (mask & (1 << i))]
        return seq, mask, pressed, ax, ay
    return None

def handle_gamepad_state(seq, mask, pressed, ax, ay, mode_tag):
    sys.stdout.write(f"\r[{mode_tag}] Seq:{seq:05d} | Stick:({ax:+04d},{ay:+04d}) | Btns: {' '.join(pressed) if pressed else '---'}        ")
    sys.stdout.flush()

    if UINPUT_AVAILABLE:
        # Map to uinput gamepad
        device.emit(uinput.ABS_X, ax, syn=False)
        device.emit(uinput.ABS_Y, ay, syn=False)
        device.emit(uinput.BTN_A, 1 if (mask & (1 << 6)) else 0, syn=False) # Cross
        device.emit(uinput.BTN_B, 1 if (mask & (1 << 5)) else 0, syn=False) # Circle
        device.emit(uinput.BTN_X, 1 if (mask & (1 << 7)) else 0, syn=False) # Square
        device.emit(uinput.BTN_Y, 1 if (mask & (1 << 4)) else 0, syn=False) # Triangle
        device.emit(uinput.BTN_TL, 1 if (mask & (1 << 8)) else 0, syn=False) # L
        device.emit(uinput.BTN_TR, 1 if (mask & (1 << 9)) else 0, syn=False) # R
        device.emit(uinput.BTN_SELECT, 1 if (mask & (1 << 10)) else 0, syn=False)
        device.emit(uinput.BTN_START, 1 if (mask & (1 << 11)) else 0, syn=True)

# 1. UDP Server (WiFi and broadcast discovery)
def run_udp_server():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(("0.0.0.0", PORT))
    print(f"[NEMPSP] WiFi UDP Listener actif sur 0.0.0.0:{PORT}")

    while True:
        try:
            data, addr = sock.recvfrom(512)
            if data == b"NEMPSP_DISCOVERY_REQUEST":
                # Reply to broadcast discovery
                sock.sendto(b"NEMPSP_SERVER:CHROMEBOOK_ACTIVE", addr)
                continue

            parsed = parse_packet(data)
            if parsed:
                seq, mask, pressed, ax, ay = parsed
                handle_gamepad_state(seq, mask, pressed, ax, ay, "WIFI")
        except Exception as e:
            print(f"[UDP Error] {e}")

# 2. TCP Server (USB ADB Loopback / Direct TCP)
def handle_tcp_client(conn, addr):
    print(f"\n[NEMPSP] Client USB/TCP connecté depuis {addr}")
    buffer = bytearray()
    while True:
        try:
            chunk = conn.recv(64)
            if not chunk:
                break
            if b"PING" in chunk:
                conn.sendall(b"PONG\n")
                continue
            buffer.extend(chunk)
            while len(buffer) >= 9:
                if buffer[0] == 0x4E and buffer[1] == 0x4D:
                    packet = buffer[:9]
                    parsed = parse_packet(packet)
                    if parsed:
                        seq, mask, pressed, ax, ay = parsed
                        handle_gamepad_state(seq, mask, pressed, ax, ay, "USB")
                    buffer = buffer[9:]
                else:
                    buffer.pop(0)
        except Exception:
            break
    conn.close()
    print("\n[NEMPSP] Client USB/TCP déconnecté.")

def run_tcp_server():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("0.0.0.0", PORT))
    server.listen(5)
    print(f"[NEMPSP] USB/TCP Listener actif sur 0.0.0.0:{PORT}")
    while True:
        conn, addr = server.accept()
        t = threading.Thread(target=handle_tcp_client, args=(conn, addr), daemon=True)
        t.start()

if __name__ == "__main__":
    print("==================================================")
    print("      NEMPSP SERVEUR CHROMEBOOK & PC DÉMARRÉ      ")
    print("==================================================")
    hostname = socket.gethostname()
    try:
        local_ip = socket.gethostbyname(hostname)
        print(f"IP Locale Chromebook : {local_ip}")
    except Exception:
        pass
    print(f"Port d'écoute        : {PORT}")
    print("--------------------------------------------------")
    print("Pour connexion USB : adb reverse tcp:8989 tcp:8989")
    print("--------------------------------------------------")

    t1 = threading.Thread(target=run_udp_server, daemon=True)
    t2 = threading.Thread(target=run_tcp_server, daemon=True)
    t1.start()
    t2.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n[NEMPSP] Arrêt du serveur.")
"""

    const val CHROMEBOOK_INSTRUCTIONS = """
GUIDE D'INSTALLATION SUR CHROMEBOOK :

1. Activez Linux sur votre Chromebook :
   - Ouvrez Paramètres > Développeurs > Environnement de développement Linux (Crostini) > Activer.

2. Ouvrez le Terminal Linux du Chromebook et exécutez :
   sudo apt update
   sudo apt install -y python3 python3-pip

3. Téléchargez ou collez le script nempsp_server.py :
   nano nempsp_server.py  (puis collez le code et Ctrl+O, Entrée, Ctrl+X)

4. Lancez le serveur :
   python3 nempsp_server.py

5. Pour jouer avec PPSSPP :
   - Installez PPSSPP (disponible via Flatpak ou sur le Play Store Chromebook).
   - Les touches du contrôleur virtuel sont reconnues immédiatement !
   - Si vous utilisez le mode USB avec un câble, tapez dans le terminal :
     adb reverse tcp:8989 tcp:8989
"""
}
