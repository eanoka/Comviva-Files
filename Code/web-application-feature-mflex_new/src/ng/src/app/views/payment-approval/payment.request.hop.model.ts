import { WorkflowHop } from "../account-management/workflow-hop.model";
import {Client} from "../manage-bill-data/client.model";
import {User} from "../user-management/user.model";

export class PaymentRequestHop {
    public id: number
    public initiationTime: Date
    public executionTime: Date
    public executedBy: User
    public previousHop: PaymentRequestHop
    public possibleExecutors: User[]
    public serviceCharge: number
    public account: Client
    public workflowHop: WorkflowHop
    public attachment : string
    public comment: string
}