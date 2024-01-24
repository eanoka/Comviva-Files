import {Client} from "../manage-bill-data/client.model";
import {Division} from "../manage-bill-data/division.model";
import {Role} from "../role-management/role.model";
import {User} from "./user.model";

export class CreateFormInitializationData {
    public clients: Client[];
    public divisions: Division[];
    public roles: Role[];
    public user: User;
}