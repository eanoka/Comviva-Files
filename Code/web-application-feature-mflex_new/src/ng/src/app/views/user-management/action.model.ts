export class AllowDeny {
    public name: string;
    public allow: boolean
    public deny: boolean

    public equals(x: any): boolean {
        if(x.hasOwnProperty("name")) {
            return this.name == x.name
        }
        return x == this.name
    }
}