import {Division} from "./division.model";

export class Client {
    public id: number;
    public name: string;
    public msisdn: number;
    public address1: number;
    public address2: number;
    public active: boolean;
    public description: string;
    public clientDivisions: Division[]

    public equals(o: any): boolean {
        return this.id == o.id
    }
}