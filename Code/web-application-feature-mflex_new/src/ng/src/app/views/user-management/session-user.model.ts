import {Client} from "../manage-bill-data/client.model";
import {Role} from "../role-management/role.model";

export class SessionUser {
    session: string
    name: string
    id: number
    email: string
    adid: string
    permissions: string[]
    isGP: boolean
    client: Client
    role: Role
}