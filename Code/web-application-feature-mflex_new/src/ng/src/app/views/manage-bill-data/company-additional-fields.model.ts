import { BilldataAdditionalField } from "./billdata-additional-field";
import {Company} from "./company.model";

export class CompanyAdditionalFields 
{
    public id: number;
    public company: Company;
    public paramCode: string;
    public paramName: string;
    public paramType: string;
    public paramLength: number;
    public paramValues: string;
    public validationRegex: string;
    public required: string;
    public errorMessage: string
    public status: string;
    public configs: string;
    public billdataInput: boolean;
    public defaultValue: any;
    public bill: BilldataAdditionalField[] = []
}