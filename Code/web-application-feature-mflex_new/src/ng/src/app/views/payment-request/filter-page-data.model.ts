import {Category} from "../manage-bill-data/category.model";
import {Division} from "../manage-bill-data/division.model";
import {Client} from "../manage-bill-data/client.model";

export class FilterPageData {
    public categories: Category[] = [];
    public divisions: Division[] = [];
    public clients: Client[]
}