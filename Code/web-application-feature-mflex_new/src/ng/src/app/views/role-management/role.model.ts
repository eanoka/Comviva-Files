export class Role {
    public id: number;
    public name: string;
    public readonly: boolean;
    public isForGp: boolean;
    public inheritedFrom: Role

    public equals(r: Role | number) {
        if(r == null) {
            return false
        }
        if(typeof r === "number") {
            return r == this.id
        } else {
            return r.id == this.id
        }
        return false
    }
}