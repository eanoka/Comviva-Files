declare global {
    interface Number {
        toBdFormat: () => string;
    }

    interface Array<T> {
        findSimilar: (T, equalityChecker?: Function) => T
    }
}

export function ExtendGlobals() {
    Number.prototype.toBdFormat = function() {
        let fixed = this.toFixed(2);
        let hasMinus = false
        if(fixed.startsWith("-")) {
            hasMinus = true
            fixed = fixed.substring(1)
        }
        let total = fixed.length - 6
        let returnable = ""
        if(total <= 0) {
            return (hasMinus ? "-" : "") + fixed
        }
        let last6 = fixed.substring(total)
        fixed = fixed.substring(0, total)
        if(total % 2 != 0) {
            returnable = fixed.charAt(0) + ","
            fixed = fixed.substring(1)
        }
        let group = /\d\d/g
        let match
        while((match = group.exec(fixed)) != null) {
            returnable += match + ","
        }
        return (hasMinus ? "-" : "") + returnable + last6
    }

    Array.prototype.findSimilar = function<T>(t: T, equalityChecker: Function = null): T {
        for(let x of this) {
            if(equalityChecker) {
                if(equalityChecker.call(x, t)) {
                    return x
                }
            } else if(x.equals) {
                if(x.equals(t)) {
                    return x
                }
            } else {
                if(x == t) {
                    return x
                }
            }
        }
        return null
    }
}