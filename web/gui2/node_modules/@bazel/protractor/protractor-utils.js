/**
 * @license
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
(function (factory) {
    if (typeof module === "object" && typeof module.exports === "object") {
        var v = factory(require, exports);
        if (v !== undefined) module.exports = v;
    }
    else if (typeof define === "function" && define.amd) {
        define("@bazel/protractor/protractor-utils", ["require", "exports", "child_process", "net"], factory);
    }
})(function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    const child_process = require("child_process");
    const net = require("net");
    function isTcpPortFree(port) {
        return new Promise((resolve, reject) => {
            const server = net.createServer();
            server.on('error', (e) => {
                resolve(false);
            });
            server.on('close', () => {
                resolve(true);
            });
            server.listen(port, () => {
                server.close();
            });
        });
    }
    exports.isTcpPortFree = isTcpPortFree;
    function isTcpPortBound(port) {
        return new Promise((resolve, reject) => {
            const client = new net.Socket();
            client.once('connect', () => {
                resolve(true);
            });
            client.once('error', (e) => {
                resolve(false);
            });
            client.connect(port);
        });
    }
    exports.isTcpPortBound = isTcpPortBound;
    function findFreeTcpPort() {
        return __awaiter(this, void 0, void 0, function* () {
            const range = {
                min: 32768,
                max: 60000,
            };
            for (let i = 0; i < 100; i++) {
                let port = Math.floor(Math.random() * (range.max - range.min) + range.min);
                if (yield isTcpPortFree(port)) {
                    return port;
                }
            }
            throw new Error('Unable to find a free port');
        });
    }
    exports.findFreeTcpPort = findFreeTcpPort;
    function waitForServer(port, timeout) {
        return isTcpPortBound(port).then(isBound => {
            if (!isBound) {
                if (timeout <= 0) {
                    throw new Error('Timeout waiting for server to start');
                }
                const wait = Math.min(timeout, 500);
                return new Promise((res, rej) => setTimeout(res, wait))
                    .then(() => waitForServer(port, timeout - wait));
            }
            return true;
        });
    }
    exports.waitForServer = waitForServer;
    /**
     * Runs the specified server binary from a given workspace and waits for the server
     * being ready. The server binary will be resolved from the Bazel runfiles. Note that
     * the server will be launched with a random free port in order to support test concurrency
     * with Bazel.
     */
    function runServer(workspace, serverTarget, portFlag, serverArgs, timeout = 5000) {
        return __awaiter(this, void 0, void 0, function* () {
            const serverPath = require.resolve(`${workspace}/${serverTarget}`);
            const port = yield findFreeTcpPort();
            // Start the Bazel server binary with a random free TCP port.
            const serverProcess = child_process.spawn(serverPath, serverArgs.concat([portFlag, port.toString()]), { stdio: 'inherit' });
            // In case the process exited with an error, we want to propagate the error.
            serverProcess.on('exit', exitCode => {
                if (exitCode !== 0) {
                    throw new Error(`Server exited with error code: ${exitCode}`);
                }
            });
            // Wait for the server to be bound to the given port.
            yield waitForServer(port, timeout);
            return { port };
        });
    }
    exports.runServer = runServer;
});
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoicHJvdHJhY3Rvci11dGlscy5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbIi4uLy4uLy4uLy4uLy4uL2V4dGVybmFsL25wbV9iYXplbF9wcm90cmFjdG9yL3Byb3RyYWN0b3ItdXRpbHMudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IkFBQUE7Ozs7Ozs7Ozs7Ozs7OztHQWVHOzs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7SUFFSCwrQ0FBK0M7SUFDL0MsMkJBQTJCO0lBRTNCLFNBQWdCLGFBQWEsQ0FBQyxJQUFZO1FBQ3hDLE9BQU8sSUFBSSxPQUFPLENBQUMsQ0FBQyxPQUFPLEVBQUUsTUFBTSxFQUFFLEVBQUU7WUFDckMsTUFBTSxNQUFNLEdBQUcsR0FBRyxDQUFDLFlBQVksRUFBRSxDQUFDO1lBQ2xDLE1BQU0sQ0FBQyxFQUFFLENBQUMsT0FBTyxFQUFFLENBQUMsQ0FBQyxFQUFFLEVBQUU7Z0JBQ3ZCLE9BQU8sQ0FBQyxLQUFLLENBQUMsQ0FBQztZQUNqQixDQUFDLENBQUMsQ0FBQztZQUNILE1BQU0sQ0FBQyxFQUFFLENBQUMsT0FBTyxFQUFFLEdBQUcsRUFBRTtnQkFDdEIsT0FBTyxDQUFDLElBQUksQ0FBQyxDQUFDO1lBQ2hCLENBQUMsQ0FBQyxDQUFDO1lBQ0gsTUFBTSxDQUFDLE1BQU0sQ0FBQyxJQUFJLEVBQUUsR0FBRyxFQUFFO2dCQUN2QixNQUFNLENBQUMsS0FBSyxFQUFFLENBQUM7WUFDakIsQ0FBQyxDQUFDLENBQUM7UUFDTCxDQUFDLENBQUMsQ0FBQztJQUNMLENBQUM7SUFiRCxzQ0FhQztJQUVELFNBQWdCLGNBQWMsQ0FBQyxJQUFZO1FBQ3pDLE9BQU8sSUFBSSxPQUFPLENBQUMsQ0FBQyxPQUFPLEVBQUUsTUFBTSxFQUFFLEVBQUU7WUFDckMsTUFBTSxNQUFNLEdBQUcsSUFBSSxHQUFHLENBQUMsTUFBTSxFQUFFLENBQUM7WUFDaEMsTUFBTSxDQUFDLElBQUksQ0FBQyxTQUFTLEVBQUUsR0FBRyxFQUFFO2dCQUMxQixPQUFPLENBQUMs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QUMsQ0FBQztZQUVuQyxPQUFPLEVBQUMsSUFBSSxFQUFDLENBQUM7UUFDaEIsQ0FBQztLQUFBO0lBckJELDhCQXFCQyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGxpY2Vuc2VcbiAqIENvcHlyaWdodCAyMDE3IFRoZSBCYXplbCBBdXRob3JzLiBBbGwgcmlnaHRzIHJlc2VydmVkLlxuICpcbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBBcGFjaGUgTGljZW5zZSwgVmVyc2lvbiAyLjAgKHRoZSBcIkxpY2Vuc2VcIik7XG4gKiB5b3UgbWF5IG5vdCB1c2UgdGhpcyBmaWxlIGV4Y2VwdCBpbiBjb21wbGlhbmNlIHdpdGggdGhlIExpY2Vuc2UuXG4gKlxuICogWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mIHRoZSBMaWNlbnNlIGF0XG4gKiAgICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4gKlxuICogVW5sZXNzIHJlcXVpcmVkIGJ5IGFwcGxpY2FibGUgbGF3IG9yIGFncmVlZCB0byBpbiB3cml0aW5nLCBzb2Z0d2FyZVxuICogZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuICogV0lUSE9VVCBXQVJSQU5USUVTIE9SIENPTkRJVElPTlMgT0YgQU5ZIEtJTkQsIGVpdGhlciBleHByZXNzIG9yIGltcGxpZWQuXG4gKiBTZWUgdGhlIExpY2Vuc2UgZm9yIHRoZSBzcGVjaWZpYyBsYW5ndWFnZSBnb3Zlcm5pbmcgcGVybWlzc2lvbnMgYW5kXG4gKiBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cbiAqL1xuXG5pbXBvcnQgKiBhcyBjaGlsZF9wcm9jZXNzIGZyb20gJ2NoaWxkX3Byb2Nlc3MnO1xuaW1wb3J0ICogYXMgbmV0IGZyb20gJ25ldCc7XG5cbmV4cG9ydCBmdW5jdGlvbiBpc1RjcFBvcnRGcmVlKHBvcnQ6IG51bWJlcik6IFByb21pc2U8Ym9vbGVhbj4ge1xuICByZXR1cm4gbmV3IFByb21pc2UoKHJlc29sdmUsIHJlamVjdCkgPT4ge1xuICAgIGNvbnN0IHNlcnZlciA9IG5ldC5jcmVhdGVTZXJ2ZXIoKTtcbiAgICBzZXJ2ZXIub24oJ2Vycm9yJywgKGUpID0+IHtcbiAgICAgIHJlc29sdmUoZmFsc2UpO1xuICAgIH0pO1xuICAgIHNlcnZlci5vbignY2xvc2UnLCAoKSA9PiB7XG4gICAgICByZXNvbHZlKHRydWUpO1xuICAgIH0pO1xuICAgIHNlcnZlci5saXN0ZW4ocG9ydCwgKCkgPT4ge1xuICAgICAgc2VydmVyLmNsb3NlKCk7XG4gICAgfSk7XG4gIH0pO1xufVxuXG5leHBvcnQgZnVuY3Rpb24gaXNUY3BQb3J0Qm91bmQocG9ydDogbnVtYmVyKTogUHJvbWlzZTxib29sZWFuPiB7XG4gIHJldHVybiBuZXcgUHJvbWlzZSgocmVzb2x2ZSwgcmVqZWN0KSA9PiB7XG4gICAgY29uc3QgY2xpZW50ID0gbmV3IG5ldC5Tb2NrZXQoKTtcbiAgICBjbGllbnQub25jZSgnY29ubmVjdCcsICgpID0+IHtcbiAgICAgIHJlc29sdmUodHJ1ZSk7XG4gICAgfSk7XG4gICAgY2xpZW50Lm9uY2UoJ2Vycm9yJywgKGUpID0+IHtcbiAgICAgIHJlc29sdmUoZmFsc2UpO1xuICAgIH0pO1xuICAgIGNsaWVudC5jb25uZWN0KHBvcnQpO1xuICB9KTtcbn1cblxuZXhwb3J0IGFzeW5jIGZ1bmN0aW9uIGZpbmRGcmVlVGNwUG9ydCgpOiBQcm9taXNlPG51bWJlcj4ge1xuICBjb25zdCByYW5nZSA9IHtcbiAgICBtaW46IDMyNzY4LFxuICAgIG1heDogNjAwMDAsXG4gIH07XG4gIGZvciAobGV0IGkgPSAwOyBpIDwgMTAwOyBpKyspIHtcbiAgICBsZXQgcG9ydCA9IE1hdGguZmxvb3IoTWF0aC5yYW5kb20oKSAqIChyYW5nZS5tYXggLSByYW5nZS5taW4pICsgcmFuZ2UubWluKTtcbiAgICBpZiAoYXdhaXQgaXNUY3BQb3J0RnJlZShwb3J0KSkge1xuICAgICAgcmV0dXJuIHBvcnQ7XG4gICAgfVxuICB9XG4gIHRocm93IG5ldyBFcnJvcignVW5hYmxlIHRvIGZpbmQgYSBmcmVlIHBvcnQnKTtcbn1cblxuLy8gSW50ZXJmYWNlIGZvciBjb25maWcgcGFyYW1ldGVyIG9mIHRoZSBwcm90cmFjdG9yX3dlYl90ZXN0X3N1aXRlIG9uUHJlcGFyZSBmdW5jdGlvblxuZXhwb3J0IGludGVyZmFjZSBPblByZXBhcmVDb25maWcge1xuICAvLyBUaGUgd29ya3NwYWNlIG5hbWVcbiAgd29ya3NwYWNlOiBzdHJpbmc7XG5cbiAgLy8gVGhlIHNlcnZlciBiaW5hcnkgdG8gcnVuXG4gIHNlcnZlcjogc3RyaW5nO1xufVxuXG5leHBvcnQgZnVuY3Rpb24gd2FpdEZvclNlcnZlcihwb3J0OiBudW1iZXIsIHRpbWVvdXQ6IG51bWJlcik6IFByb21pc2U8Ym9vbGVhbj4ge1xuICByZXR1cm4gaXNUY3BQb3J0Qm91bmQocG9ydCkudGhlbihpc0JvdW5kID0+IHtcbiAgICBpZiAoIWlzQm91bmQpIHtcbiAgICAgIGlmICh0aW1lb3V0IDw9IDApIHtcbiAgICAgICAgdGhyb3cgbmV3IEVycm9yKCdUaW1lb3V0IHdhaXRpbmcgZm9yIHNlcnZlciB0byBzdGFydCcpO1xuICAgICAgfVxuICAgICAgY29uc3Qgd2FpdCA9IE1hdGgubWluKHRpbWVvdXQsIDUwMCk7XG4gICAgICByZXR1cm4gbmV3IFByb21pc2UoKHJlcywgcmVqKSA9PiBzZXRUaW1lb3V0KHJlcywgd2FpdCkpXG4gICAgICAgICAgLnRoZW4oKCkgPT4gd2FpdEZvclNlcnZlcihwb3J0LCB0aW1lb3V0IC0gd2FpdCkpO1xuICAgIH1cbiAgICByZXR1cm4gdHJ1ZTtcbiAgfSk7XG59XG5cbi8vIFJldHVybiB0eXBlIGZyb20gcnVuU2VydmVyIGZ1bmN0aW9uXG5leHBvcnQgaW50ZXJmYWNlIFNlcnZlclNwZWMge1xuICAvLyBQb3J0IG51bWJlciB0aGF0IHRoZSBzZXJ2ZXIgaXMgcnVubmluZyBvblxuICBwb3J0OiBudW1iZXI7XG59XG5cbi8qKlxuICogUnVucyB0aGUgc3BlY2lmaWVkIHNlcnZlciBiaW5hcnkgZnJvbSBhIGdpdmVuIHdvcmtzcGFjZSBhbmQgd2FpdHMgZm9yIHRoZSBzZXJ2ZXJcbiAqIGJlaW5nIHJlYWR5LiBUaGUgc2VydmVyIGJpbmFyeSB3aWxsIGJlIHJlc29sdmVkIGZyb20gdGhlIEJhemVsIHJ1bmZpbGVzLiBOb3RlIHRoYXRcbiAqIHRoZSBzZXJ2ZXIgd2lsbCBiZSBsYXVuY2hlZCB3aXRoIGEgcmFuZG9tIGZyZWUgcG9ydCBpbiBvcmRlciB0byBzdXBwb3J0IHRlc3QgY29uY3VycmVuY3lcbiAqIHdpdGggQmF6ZWwuXG4gKi9cbmV4cG9ydCBhc3luYyBmdW5jdGlvbiBydW5TZXJ2ZXIoXG4gICAgd29ya3NwYWNlOiBzdHJpbmcsIHNlcnZlclRhcmdldDogc3RyaW5nLCBwb3J0RmxhZzogc3RyaW5nLCBzZXJ2ZXJBcmdzOiBzdHJpbmdbXSxcbiAgICB0aW1lb3V0ID0gNTAwMCk6IFByb21pc2U8U2VydmVyU3BlYz4ge1xuICBjb25zdCBzZXJ2ZXJQYXRoID0gcmVxdWlyZS5yZXNvbHZlKGAke3dvcmtzcGFjZX0vJHtzZXJ2ZXJUYXJnZXR9YCk7XG4gIGNvbnN0IHBvcnQgPSBhd2FpdCBmaW5kRnJlZVRjcFBvcnQoKTtcblxuICAvLyBTdGFydCB0aGUgQmF6ZWwgc2VydmVyIGJpbmFyeSB3aXRoIGEgcmFuZG9tIGZyZWUgVENQIHBvcnQuXG4gIGNvbnN0IHNlcnZlclByb2Nlc3MgPSBjaGlsZF9wcm9jZXNzLnNwYXduKFxuICAgICAgc2VydmVyUGF0aCwgc2VydmVyQXJncy5jb25jYXQoW3BvcnRGbGFnLCBwb3J0LnRvU3RyaW5nKCldKSwge3N0ZGlvOiAnaW5oZXJpdCd9KTtcblxuICAvLyBJbiBjYXNlIHRoZSBwcm9jZXNzIGV4aXRlZCB3aXRoIGFuIGVycm9yLCB3ZSB3YW50IHRvIHByb3BhZ2F0ZSB0aGUgZXJyb3IuXG4gIHNlcnZlclByb2Nlc3Mub24oJ2V4aXQnLCBleGl0Q29kZSA9PiB7XG4gICAgaWYgKGV4aXRDb2RlICE9PSAwKSB7XG4gICAgICB0aHJvdyBuZXcgRXJyb3IoYFNlcnZlciBleGl0ZWQgd2l0aCBlcnJvciBjb2RlOiAke2V4aXRDb2RlfWApO1xuICAgIH1cbiAgfSk7XG5cbiAgLy8gV2FpdCBmb3IgdGhlIHNlcnZlciB0byBiZSBib3VuZCB0byB0aGUgZ2l2ZW4gcG9ydC5cbiAgYXdhaXQgd2FpdEZvclNlcnZlcihwb3J0LCB0aW1lb3V0KTtcblxuICByZXR1cm4ge3BvcnR9O1xufVxuIl19