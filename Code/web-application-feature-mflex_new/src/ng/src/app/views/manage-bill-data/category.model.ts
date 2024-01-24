import {Company} from "./company.model";

export class Category {
    public id: number;
    public code: string;
    public name: string;
    public companies: Company[];
}