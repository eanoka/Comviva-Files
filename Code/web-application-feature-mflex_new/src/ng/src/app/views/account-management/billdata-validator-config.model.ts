import { Role } from "../role-management/role.model";
import { User } from "../user-management/user.model";

export class BilldataValidatorConfigResponse {
    public isEnabled: boolean = false;
    public allowedRoles: Role[] = []
    public allowedUsers: User[] = []
    public allRoles: Role[] = []
    public allUsers: User[] = []
}

export class BilldataValidatorConfig {
    public isEnabled: boolean = false;
    public allowedRoles: Role[] = []
    public allowedUsers: User[] = []
}