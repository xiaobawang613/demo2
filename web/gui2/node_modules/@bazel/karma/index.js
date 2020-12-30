(function (factory) {
    if (typeof module === "object" && typeof module.exports === "object") {
        var v = factory(require, exports);
        if (v !== undefined) module.exports = v;
    }
    else if (typeof define === "function" && define.amd) {
        define("@bazel/karma", ["require", "exports", "crypto", "fs", "path", "process", "readline", "tmp"], factory);
    }
})(function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    /*
     * Concat all JS files before serving.
     */
    const crypto = require("crypto");
    const fs = require("fs");
    const path = require("path");
    const process = require("process");
    const readline_1 = require("readline");
    const tmp = require("tmp");
    ///<reference types="lib.dom"/>
    /**
     * Return SHA1 of data buffer.
     */
    function sha1(data) {
        const hash = crypto.createHash('sha1');
        hash.update(data);
        return hash.digest('hex');
    }
    /**
     * Entry-point for the Karma plugin.
     */
    function initConcatJs(logger, emitter, basePath, hostname, port) {
        const log = logger.create('framework.concat_js');
        // Create a tmp file for the concat bundle that is automatically cleaned up on
        // exit.
        const tmpFile = tmp.fileSync({ keep: false, dir: process.env['TEST_TMPDIR'] });
        emitter.on('file_list_modified', files => {
            const bundleFile = {
                path: '/concatjs_bundle.js',
                contentPath: tmpFile.name,
                isUrl: false,
                content: '',
                encodings: {},
            };
            // Preserve all non-JS that were there in the included list.
            const included = files.included.filter(f => path.extname(f.originalPath) !== '.js');
            const bundledFiles = files.included.filter(f => path.extname(f.originalPath) === '.js').map((file) => {
                const relativePath = path.relative(basePath, file.originalPath).replace(/\\/g, '/');
                let content = file.content + `\n//# sourceURL=http://${hostname}:${port}/base/` +
                    relativePath + '\n';
                return `
  loadFile(
      ${JSON.stringify(relativePath)},
      ${JSON.stringify(content)});`;
            });
            // Execute each file by putting it in a <script> tag. This makes them create
            // global variables, even with 'use strict'; (unlike eval).
            bundleFile.content = `
(function() {  // Hide local variables
  // IE 8 and below do not support document.head.
  var parent = document.getElementsByTagName('head')[0] ||
                    document.documentElement;
  function loadFile(path, src) {
    try {
      var script = document.createElement('script');
      if ('textContent' in script) {
        script.textContent = src;
      } else {
        // This is for IE 8 and below.
        script.text = src;
      }
      parent.appendChild(script);
      // Don't pollute the DOM with hundreds of <script> tags.
      parent.removeChild(script);
    } catch(err) {
      window.__karma__ && window.__karma__.error(
          'An error occurred while loading ' + path + ':\\n' +
          (err.stack || err.message || err.toString()));
      console.error('An error occurred while loading ' + path, err);
      throw err;
    }
  }
${bundledFiles.join('')}
})();`;
            bundleFile.sha = sha1(Buffer.from(bundleFile.content));
            bundleFile.mtime = new Date();
            included.unshift(bundleFile);
            files.included = included;
            files.served.push(bundleFile);
            log.debug('Writing concatjs bundle to tmp file %s', bundleFile.contentPath);
            fs.writeFileSync(bundleFile.contentPath, bundleFile.content);
        });
    }
    initConcatJs.$inject =
        ['logger', 'emitter', 'config.basePath', 'config.hostname', 'config.port'];
    function watcher(fileList) {
        // ibazel will write this string after a successful build
        // We don't want to re-trigger tests if the compilation fails, so
        // we should only listen for this event.
        const IBAZEL_NOTIFY_BUILD_SUCCESS = 'IBAZEL_BUILD_COMPLETED SUCCESS';
        // ibazel communicates with us via stdin
        const rl = readline_1.createInterface({ input: process.stdin, terminal: false });
        rl.on('line', (chunk) => {
            if (chunk === IBAZEL_NOTIFY_BUILD_SUCCESS) {
                fileList.refresh();
            }
        });
        rl.on('close', () => {
            // Give ibazel 5s to kill our process, otherwise do it ourselves
            setTimeout(() => {
                console.error('ibazel failed to stop karma after 5s; probably a bug');
                process.exit(1);
            }, 5000);
        });
    }
    watcher.$inject = ['fileList'];
    module.exports = {
        'framework:concat_js': ['factory', initConcatJs],
        'watcher': ['value', watcher],
    };
});
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiaW5kZXguanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyIuLi8uLi8uLi8uLi8uLi9leHRlcm5hbC9ucG1fYmF6ZWxfa2FybWEvaW5kZXgudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7Ozs7Ozs7Ozs7SUFBQTs7T0FFRztJQUNILGlDQUFpQztJQUNqQyx5QkFBeUI7SUFDekIsNkJBQTZCO0lBQzdCLG1DQUFtQztJQUNuQyx1Q0FBeUM7SUFDekMsMkJBQTJCO0lBQzNCLCtCQUErQjtJQUUvQjs7T0FFRztJQUNILFNBQVMsSUFBSSxDQUFDLElBQUk7UUFDaEIsTUFBTSxJQUFJLEdBQUcsTUFBTSxDQUFDLFVBQVUsQ0FBQyxNQUFNLENBQUMsQ0FBQztRQUN2QyxJQUFJLENBQUMsTUFBTSxDQUFDLElBQUksQ0FBQyxDQUFDO1FBQ2xCLE9BQU8sSUFBSSxDQUFDLE1BQU0sQ0FBQyxLQUFLLENBQUMsQ0FBQztJQUM1QixDQUFDO0lBRUQ7O09BRUc7SUFDSCxTQUFTLFlBQVksQ0FBQyxNQUFNLEVBQUUsT0FBTyxFQUFFLFFBQVEsRUFBRSxRQUFRLEVBQUUsSUFBSTtRQUM3RCxNQUFNLEdBQUcsR0FBRyxNQUFNLENBQUMsTUFBTSxDQUFDLHFCQUFxQixDQUFDLENBQUM7UUFFakQsOEVBQThFO1FBQzlFLFFBQVE7UUFDUixNQUFNLE9BQU8sR0FBRyxHQUFHLENBQUMsUUFBUSxDQUFDLEVBQUMsSUFBSSxFQUFFLEtBQUssRUFBRSxHQUFHLEVBQUUsT0FBTyxDQUFDLEdBQUcsQ0FBQyxhQUFhLENBQUMsRUFBQyxDQUFDLENBQUM7UUFFN0UsT0FBTyxDQUFDLEVBQUUsQ0FBQyxvQkFBb0IsRUFBRSxLQUFLLENBQUMsRUFBRTtZQUN2QyxNQUFNLFVBQVUsR0FBRztnQkFDakIsSUFBSSxFQUFFLHFCQUFxQjtnQkFDM0IsV0FBVyxFQUFFLE9BQU8sQ0FBQyxJQUFJO2dCQUN6QixLQUFLLEVBQUUsS0FBSztnQkFDWixPQUFPLEVBQUUsRUFBRTtnQkFDWCxTQUFTLEVB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Q0FBQyxDQUFDO1lBQ2xCLENBQUMsRUFBRSxJQUFJLENBQUMsQ0FBQztRQUNYLENBQUMsQ0FBQyxDQUFDO0lBQ0wsQ0FBQztJQUVBLE9BQWUsQ0FBQyxPQUFPLEdBQUcsQ0FBQyxVQUFVLENBQUMsQ0FBQztJQUV4QyxNQUFNLENBQUMsT0FBTyxHQUFHO1FBQ2YscUJBQXFCLEVBQUUsQ0FBQyxTQUFTLEVBQUUsWUFBWSxDQUFDO1FBQ2hELFNBQVMsRUFBRSxDQUFDLE9BQU8sRUFBRSxPQUFPLENBQUM7S0FDOUIsQ0FBQyIsInNvdXJjZXNDb250ZW50IjpbIi8qXG4gKiBDb25jYXQgYWxsIEpTIGZpbGVzIGJlZm9yZSBzZXJ2aW5nLlxuICovXG5pbXBvcnQgKiBhcyBjcnlwdG8gZnJvbSAnY3J5cHRvJztcbmltcG9ydCAqIGFzIGZzIGZyb20gJ2ZzJztcbmltcG9ydCAqIGFzIHBhdGggZnJvbSAncGF0aCc7XG5pbXBvcnQgKiBhcyBwcm9jZXNzIGZyb20gJ3Byb2Nlc3MnO1xuaW1wb3J0IHtjcmVhdGVJbnRlcmZhY2V9IGZyb20gJ3JlYWRsaW5lJztcbmltcG9ydCAqIGFzIHRtcCBmcm9tICd0bXAnO1xuLy8vPHJlZmVyZW5jZSB0eXBlcz1cImxpYi5kb21cIi8+XG5cbi8qKlxuICogUmV0dXJuIFNIQTEgb2YgZGF0YSBidWZmZXIuXG4gKi9cbmZ1bmN0aW9uIHNoYTEoZGF0YSkge1xuICBjb25zdCBoYXNoID0gY3J5cHRvLmNyZWF0ZUhhc2goJ3NoYTEnKTtcbiAgaGFzaC51cGRhdGUoZGF0YSk7XG4gIHJldHVybiBoYXNoLmRpZ2VzdCgnaGV4Jyk7XG59XG5cbi8qKlxuICogRW50cnktcG9pbnQgZm9yIHRoZSBLYXJtYSBwbHVnaW4uXG4gKi9cbmZ1bmN0aW9uIGluaXRDb25jYXRKcyhsb2dnZXIsIGVtaXR0ZXIsIGJhc2VQYXRoLCBob3N0bmFtZSwgcG9ydCkge1xuICBjb25zdCBsb2cgPSBsb2dnZXIuY3JlYXRlKCdmcmFtZXdvcmsuY29uY2F0X2pzJyk7XG5cbiAgLy8gQ3JlYXRlIGEgdG1wIGZpbGUgZm9yIHRoZSBjb25jYXQgYnVuZGxlIHRoYXQgaXMgYXV0b21hdGljYWxseSBjbGVhbmVkIHVwIG9uXG4gIC8vIGV4aXQuXG4gIGNvbnN0IHRtcEZpbGUgPSB0bXAuZmlsZVN5bmMoe2tlZXA6IGZhbHNlLCBkaXI6IHByb2Nlc3MuZW52WydURVNUX1RNUERJUiddfSk7XG5cbiAgZW1pdHRlci5vbignZmlsZV9saXN0X21vZGlmaWVkJywgZmlsZXMgPT4ge1xuICAgIGNvbnN0IGJ1bmRsZUZpbGUgPSB7XG4gICAgICBwYXRoOiAnL2NvbmNhdGpzX2J1bmRsZS5qcycsXG4gICAgICBjb250ZW50UGF0aDogdG1wRmlsZS5uYW1lLFxuICAgICAgaXNVcmw6IGZhbHNlLFxuICAgICAgY29udGVudDogJycsXG4gICAgICBlbmNvZGluZ3M6IHt9LFxuICAgIH0gYXMgYW55O1xuICAgIC8vIFByZXNlcnZlIGFsbCBub24tSlMgdGhhdCB3ZXJlIHRoZXJlIGluIHRoZSBpbmNsdWRlZCBsaXN0LlxuICAgIGNvbnN0IGluY2x1ZGVkID0gZmlsZXMuaW5jbHVkZWQuZmlsdGVyKGYgPT4gcGF0aC5leHRuYW1lKGYub3JpZ2luYWxQYXRoKSAhPT0gJy5qcycpO1xuICAgIGNvbnN0IGJ1bmRsZWRGaWxlcyA9XG4gICAgICAgIGZpbGVzLmluY2x1ZGVkLmZpbHRlcihmID0+IHBhdGguZXh0bmFtZShmLm9yaWdpbmFsUGF0aCkgPT09ICcuanMnKS5tYXAoKGZpbGUpID0+IHtcbiAgICAgICAgICBjb25zdCByZWxhdGl2ZVBhdGggPSBwYXRoLnJlbGF0aXZlKGJhc2VQYXRoLCBmaWxlLm9yaWdpbmFsUGF0aCkucmVwbGFjZSgvXFxcXC9nLCAnLycpO1xuXG4gICAgICAgICAgbGV0IGNvbnRlbnQgPSBmaWxlLmNvbnRlbnQgKyBgXFxuLy8jIHNvdXJjZVVSTD1odHRwOi8vJHtob3N0bmFtZX06JHtwb3J0fS9iYXNlL2AgK1xuICAgICAgICAgICAgICByZWxhdGl2ZVBhdGggKyAnXFxuJztcblxuICAgICAgICAgIHJldHVybiBgXG4gIGxvYWRGaWxlKFxuICAgICAgJHtKU09OLnN0cmluZ2lmeShyZWxhdGl2ZVBhdGgpfSxcbiAgICAgICR7SlNPTi5zdHJpbmdpZnkoY29udGVudCl9KTtgO1xuICAgICAgICB9KTtcblxuICAgIC8vIEV4ZWN1dGUgZWFjaCBmaWxlIGJ5IHB1dHRpbmcgaXQgaW4gYSA8c2NyaXB0PiB0YWcuIFRoaXMgbWFrZXMgdGhlbSBjcmVhdGVcbiAgICAvLyBnbG9iYWwgdmFyaWFibGVzLCBldmVuIHdpdGggJ3VzZSBzdHJpY3QnOyAodW5saWtlIGV2YWwpLlxuICAgIGJ1bmRsZUZpbGUuY29udGVudCA9IGBcbihmdW5jdGlvbigpIHsgIC8vIEhpZGUgbG9jYWwgdmFyaWFibGVzXG4gIC8vIElFIDggYW5kIGJlbG93IGRvIG5vdCBzdXBwb3J0IGRvY3VtZW50LmhlYWQuXG4gIHZhciBwYXJlbnQgPSBkb2N1bWVudC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnaGVhZCcpWzBdIHx8XG4gICAgICAgICAgICAgICAgICAgIGRvY3VtZW50LmRvY3VtZW50RWxlbWVudDtcbiAgZnVuY3Rpb24gbG9hZEZpbGUocGF0aCwgc3JjKSB7XG4gICAgdHJ5IHtcbiAgICAgIHZhciBzY3JpcHQgPSBkb2N1bWVudC5jcmVhdGVFbGVtZW50KCdzY3JpcHQnKTtcbiAgICAgIGlmICgndGV4dENvbnRlbnQnIGluIHNjcmlwdCkge1xuICAgICAgICBzY3JpcHQudGV4dENvbnRlbnQgPSBzcmM7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICAvLyBUaGlzIGlzIGZvciBJRSA4IGFuZCBiZWxvdy5cbiAgICAgICAgc2NyaXB0LnRleHQgPSBzcmM7XG4gICAgICB9XG4gICAgICBwYXJlbnQuYXBwZW5kQ2hpbGQoc2NyaXB0KTtcbiAgICAgIC8vIERvbid0IHBvbGx1dGUgdGhlIERPTSB3aXRoIGh1bmRyZWRzIG9mIDxzY3JpcHQ+IHRhZ3MuXG4gICAgICBwYXJlbnQucmVtb3ZlQ2hpbGQoc2NyaXB0KTtcbiAgICB9IGNhdGNoKGVycikge1xuICAgICAgd2luZG93Ll9fa2FybWFfXyAmJiB3aW5kb3cuX19rYXJtYV9fLmVycm9yKFxuICAgICAgICAgICdBbiBlcnJvciBvY2N1cnJlZCB3aGlsZSBsb2FkaW5nICcgKyBwYXRoICsgJzpcXFxcbicgK1xuICAgICAgICAgIChlcnIuc3RhY2sgfHwgZXJyLm1lc3NhZ2UgfHwgZXJyLnRvU3RyaW5nKCkpKTtcbiAgICAgIGNvbnNvbGUuZXJyb3IoJ0FuIGVycm9yIG9jY3VycmVkIHdoaWxlIGxvYWRpbmcgJyArIHBhdGgsIGVycik7XG4gICAgICB0aHJvdyBlcnI7XG4gICAgfVxuICB9XG4ke2J1bmRsZWRGaWxlcy5qb2luKCcnKX1cbn0pKCk7YDtcbiAgICBidW5kbGVGaWxlLnNoYSA9IHNoYTEoQnVmZmVyLmZyb20oYnVuZGxlRmlsZS5jb250ZW50KSk7XG4gICAgYnVuZGxlRmlsZS5tdGltZSA9IG5ldyBEYXRlKCk7XG4gICAgaW5jbHVkZWQudW5zaGlmdChidW5kbGVGaWxlKTtcblxuICAgIGZpbGVzLmluY2x1ZGVkID0gaW5jbHVkZWQ7XG4gICAgZmlsZXMuc2VydmVkLnB1c2goYnVuZGxlRmlsZSk7XG5cbiAgICBsb2cuZGVidWcoJ1dyaXRpbmcgY29uY2F0anMgYnVuZGxlIHRvIHRtcCBmaWxlICVzJywgYnVuZGxlRmlsZS5jb250ZW50UGF0aCk7XG4gICAgZnMud3JpdGVGaWxlU3luYyhidW5kbGVGaWxlLmNvbnRlbnRQYXRoLCBidW5kbGVGaWxlLmNvbnRlbnQpO1xuICB9KTtcbn1cblxuKGluaXRDb25jYXRKcyBhcyBhbnkpLiRpbmplY3QgPVxuICAgIFsnbG9nZ2VyJywgJ2VtaXR0ZXInLCAnY29uZmlnLmJhc2VQYXRoJywgJ2NvbmZpZy5ob3N0bmFtZScsICdjb25maWcucG9ydCddO1xuXG5mdW5jdGlvbiB3YXRjaGVyKGZpbGVMaXN0OiB7cmVmcmVzaDogKCkgPT4gdm9pZH0pIHtcbiAgLy8gaWJhemVsIHdpbGwgd3JpdGUgdGhpcyBzdHJpbmcgYWZ0ZXIgYSBzdWNjZXNzZnVsIGJ1aWxkXG4gIC8vIFdlIGRvbid0IHdhbnQgdG8gcmUtdHJpZ2dlciB0ZXN0cyBpZiB0aGUgY29tcGlsYXRpb24gZmFpbHMsIHNvXG4gIC8vIHdlIHNob3VsZCBvbmx5IGxpc3RlbiBmb3IgdGhpcyBldmVudC5cbiAgY29uc3QgSUJBWkVMX05PVElGWV9CVUlMRF9TVUNDRVNTID0gJ0lCQVpFTF9CVUlMRF9DT01QTEVURUQgU1VDQ0VTUyc7XG4gIC8vIGliYXplbCBjb21tdW5pY2F0ZXMgd2l0aCB1cyB2aWEgc3RkaW5cbiAgY29uc3QgcmwgPSBjcmVhdGVJbnRlcmZhY2Uoe2lucHV0OiBwcm9jZXNzLnN0ZGluLCB0ZXJtaW5hbDogZmFsc2V9KTtcbiAgcmwub24oJ2xpbmUnLCAoY2h1bms6IHN0cmluZykgPT4ge1xuICAgIGlmIChjaHVuayA9PT0gSUJBWkVMX05PVElGWV9CVUlMRF9TVUNDRVNTKSB7XG4gICAgICBmaWxlTGlzdC5yZWZyZXNoKCk7XG4gICAgfVxuICB9KTtcbiAgcmwub24oJ2Nsb3NlJywgKCkgPT4ge1xuICAgIC8vIEdpdmUgaWJhemVsIDVzIHRvIGtpbGwgb3VyIHByb2Nlc3MsIG90aGVyd2lzZSBkbyBpdCBvdXJzZWx2ZXNcbiAgICBzZXRUaW1lb3V0KCgpID0+IHtcbiAgICAgIGNvbnNvbGUuZXJyb3IoJ2liYXplbCBmYWlsZWQgdG8gc3RvcCBrYXJtYSBhZnRlciA1czsgcHJvYmFibHkgYSBidWcnKTtcbiAgICAgIHByb2Nlc3MuZXhpdCgxKTtcbiAgICB9LCA1MDAwKTtcbiAgfSk7XG59XG5cbih3YXRjaGVyIGFzIGFueSkuJGluamVjdCA9IFsnZmlsZUxpc3QnXTtcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gICdmcmFtZXdvcms6Y29uY2F0X2pzJzogWydmYWN0b3J5JywgaW5pdENvbmNhdEpzXSxcbiAgJ3dhdGNoZXInOiBbJ3ZhbHVlJywgd2F0Y2hlcl0sXG59O1xuIl19