import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';


// User Management Component
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { BarUnbarUserComponent } from './bar-unbar-user.component';
import { CreateUserComponent } from './create-user.component';
import { DeleteUserComponent } from './delete-user.component';
import { EditUserComponent } from './edit-user.component';
import { UserPermsComponent } from './user-perms.component';
import { UserReportComponent } from './user-report.component';


// Buttons Routing
import { ButtonsRoutingModule } from './buttons-routing.module';
import {PaginatorModule} from "../../generic/components/paginator.module";
import { NgSelectModule } from '@ng-select/ng-select';
import {ValidatorModule} from "../../directives/validator.module";

// Angular

@NgModule({
    imports: [
        CommonModule,
        ButtonsRoutingModule,
        BsDropdownModule.forRoot(),
        FormsModule,
        PaginatorModule,
        NgSelectModule,
        ValidatorModule
    ],
  declarations: [
	BarUnbarUserComponent,
	CreateUserComponent,
	DeleteUserComponent,
	EditUserComponent,
	UserPermsComponent,
	UserReportComponent
  ]
})
export class UserManagementModule { }
