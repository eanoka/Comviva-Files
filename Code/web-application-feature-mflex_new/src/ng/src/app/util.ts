import * as j from 'jquery'
import * as moment from "moment";

const Util = {
    checkLuhnChecksum: function(value) {
        let total = 0;
        let pick = false;
        for(let x of value) {
            total += pick ? (x > 4 ? 1 + (+x - 5) * 2 : +x * 2) : +x
            pick = !pick
        }
        return total % 10 == 0
    },
    uniqueId: (function() {
        let p = 0;
        return function() {
            return ++p;
        }
    })(),
    UUID: function () {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            let r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16).toUpperCase();
        });
    },
    nop: function() {},
    omit: function (map, props, excludeFunctions) {
        if (!j.isArray(props)) {
            props = Object.keys(props)
        }
        let nmap = {}
        for (let k in map) {
            if (!props.contains(k) && (!excludeFunctions || !j.isFunction(nmap[k]))) {
                nmap[k] = map[k]
            }
        }
        return nmap;
    },
    mapToArray: function (mapOrArray, converter) {
        let nmap = []
        for (let k in mapOrArray) {
            nmap.push(converter(mapOrArray[k]))
        }
        return nmap;
    },
    onReady: function (obj, prop, callback, maxAttempt) {
        if (typeof maxAttempt == 'undefined') {
            maxAttempt = 10
        }
        if (maxAttempt > 0) {
            if (typeof obj[prop] == 'undefined') {
                if (j.isPlainObject(callback) && callback.not) {
                    callback = j.extend({}, callback)
                    callback.not.call(obj)
                    callback.not = undefined
                }
                setTimeout(function () {
                    Util.onReady(obj, prop, callback, --maxAttempt)
                }, 2000)
            } else {
                (j.isPlainObject(callback) ? callback.ready : callback).call(obj[prop])
            }
        } else {
            if (j.isPlainObject(callback) && callback.fail) {
                callback.fail.call(obj)
            }
        }
    },
    path: function (path) {
        let protocol;
        let host
        let dir
        let namePart
        let queryPart
        let fragmentPart
        if (path instanceof Location || (path.constructor && path.constructor === Location)) {
            protocol = path.protocol.substring(0, path.protocol.length - 1)
            host = path.hostname
            if (path.port) {
                host = host + ':' + path.port
            }
            let lastSlash = path.pathname.lastIndexOf('/');
            namePart = path.pathname.substring(lastSlash + 1)
            dir = lastSlash ? path.pathname.substring(1, lastSlash) : undefined
            queryPart = path.search ? path.search.substring(1) : undefined
            fragmentPart = path.hash ? path.hash.substring(1) : undefined
        } else {
            let parts;
            parts = path.split(/[\\\/]/);
            if (!parts.length) {
                return {};
            }
            if (parts[0].endsWith(':')) {
                protocol = parts[0].substring(0, parts[0].length - 1)
            }
            host = (protocol || (parts.length > 2 && parts[0] == "" && parts[1] == "")) ? parts[2] : undefined;
            namePart = parts[parts.length - 1];
            let queryIndex = namePart.indexOf('?');
            if (queryIndex > -1) {
                queryPart = namePart.substring(queryIndex + 1)
                namePart = namePart.substring(0, queryIndex)
            }
            fragmentPart = queryPart || namePart;
            let fragmentIndex = fragmentPart.indexOf('#');
            if (fragmentIndex > -1) {
                if (queryPart) {
                    queryPart = fragmentPart.substring(0, fragmentIndex)
                } else {
                    namePart = fragmentPart.substring(0, fragmentIndex)
                }
            }
            if (fragmentIndex > -1) {
                fragmentPart = fragmentPart.substring(fragmentIndex + 1)
            } else {
                fragmentPart = null;
            }
            let host_exists = protocol || (parts.length > 2 && parts[0] == "" && parts[1] == "");
            let joinableParts = parts.slice(host_exists ? 3 : 0, parts.length - 1)
            dir = joinableParts.join('/')
        }
        let dotIndex = namePart.lastIndexOf('.');
        let ext = dotIndex > -1 ? namePart.substring(dotIndex + 1) : null;
        let name = dotIndex > -1 ? namePart.substring(0, dotIndex) : namePart;
        let query = queryPart ? Util.query(queryPart) : {}
        let path_obj: any = {
            host: host,
            protocol: protocol,
            toString: function () {
                return this.full
            },
            name: name,
            ext: ext,
            query: query,
            fragment: fragmentPart,
            dir: dir
        }
        Object.defineProperty(path_obj, 'full', {
            get: function () {
                let url = path_obj.protocol ? path_obj.protocol + '://' : "";
                url = url + (path_obj.host ? (url ? "" : '//') + path_obj.host + '/' : "")
                url = url + (path_obj.dir ? path_obj.dir + '/' : "") + path_obj.file_name;
                let query = j.param(path_obj.query)
                return url + (query ? '?' + query : "") + (path_obj.fragment ? '#' + path_obj.fragment : "")
            }
        });
        Object.defineProperty(path_obj, 'file_name', {
            get: function () {
                return this.name + (this.ext ? '.' + this.ext : "")
            }
        });
        return path_obj
    },
    prop: function (obj, name, value) {
        let ids = name.split(/\./);
        let isSet = arguments.length == 3
        for (let g = 0; g < ids.length - 1; g++) {
            let nProp = "" + ids[g]
            if (!(nProp in obj)) {
                if (isSet) {
                    obj[nProp] = {}
                } else {
                    return;
                }
            }
            obj = obj[nProp];
            if (typeof obj != 'object') {
                return;
            }
        }
        if (arguments.length == 2) {
            return obj[ids[ids.length - 1]]
        } else {
            obj[ids[ids.length - 1]] = value;
        }
    },
    query: function (query) {
        let returnMap = {};
        query = decodeURIComponent(query);
        query = query.split('&');
        query.every(function (q) {
            let nameValue = q.split('=');
            nameValue[1] = nameValue[1] ? nameValue[1].replace(/\+/g, ' ') : "";
            if (returnMap[nameValue[0]] == undefined) {
                returnMap[nameValue[0]] = nameValue[1];
            } else {
                if (j.isArray(returnMap[nameValue[0]])) {
                    returnMap[nameValue[0]].push(nameValue[1]);
                } else {
                    returnMap[nameValue[0]] = [returnMap[nameValue[0]], nameValue[1]];
                }
            }
            return true
        });
        return returnMap;
    },
    saveData: function(data, filename) {
        let link = document.createElement('a')
        link.href = URL.createObjectURL(data)
        link.setAttribute('download', filename);
        link.innerHTML = 'downloading...';
        document.body.appendChild(link);
        let event = document.createEvent('MouseEvents');
        event.initMouseEvent('click', true, false, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
        link.dispatchEvent(event);
        document.body.removeChild(link);
    },
    toBCRId: function(id: number): string {
        return "BCR" + id.toString(16).padStart(8, '0').toUpperCase()
    },
    toPRId: function(id: number): string {
        return "PR" + id.toString(16).padStart(8, '0').toUpperCase()
    },
    toDate: function(dateString: string): Date {
        return moment(dateString, "DD/MM/YYYY HH:mm:ss").toDate()
    },
    getFileName: function(path: string): string {
        return path.split('\\').pop().split('/').pop();
	},
	toGbMbKb: function(bytes: number): string {
        let remBytes: number = bytes % 1024;
        let kbs: number = (bytes - remBytes) / 1024;
        let returnText: string = remBytes == 0 ? "" : (remBytes + "B");
        if(kbs == 0) {
            return returnText;
        }
        let remKbs: number = kbs % 1024;
        let mbs: number = (kbs - remKbs) / 1024;
        returnText = (remKbs == 0 ? "" : (remKbs + "KB")) + (returnText.length == 0 ? "" : (" " + returnText));
        if(mbs == 0) {
            return returnText;
        }
        let remMbs: number = mbs % 1024;
        let gbs: number = (mbs - remMbs) / 1024;
        returnText = (remMbs == 0 ? "" : (remMbs + "MB")) + (returnText.length == 0 ? "" : (" " + returnText));
        if(gbs == 0) {
            return returnText;
        }
        return gbs + "GB" + (returnText.length == 0 ? "" : (" " + returnText));
    }
}
export {Util}