import {WorkflowHop} from "./workflow-hop.model";
import {User} from "../user-management/user.model";
import {Role} from "../role-management/role.model";

export class BillpaymentApproval {
    public hops: BillpaymentApprovalHop[] = [];
    public users: User[] = [];
    public roles: Role[] = [];
    public firstLevelApprover: {users: User[], roles: Role[]} = {users: [], roles: []}
    public lastLevelApprover: {users: User[], roles: Role[]} = {users: [], roles: []}
}

export class BillpaymentApprovalHop {
    public hop: WorkflowHop;
    public users: User[];
    public roles: Role[];
}