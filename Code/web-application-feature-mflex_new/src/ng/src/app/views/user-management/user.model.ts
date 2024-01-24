import {Role} from "../role-management/role.model";
import {Client} from "../manage-bill-data/client.model";
import {Division} from "../manage-bill-data/division.model";

export class User {
    name: string
    id: number
    email: string
    adid: string
    loginId: string
    role: Role
    client: Client
    address: string
    active: boolean
    msisdn: number
    clientDivisions: Division[]
}