import {Category} from "./category.model";
import { CompanyAdditionalFields } from "./company-additional-fields.model";

export class Company {
    public id: number;
    public code: string;
    public name: string;
    public hasBill: boolean;
    public category: Category;
    public fields: CompanyAdditionalFields[]
}