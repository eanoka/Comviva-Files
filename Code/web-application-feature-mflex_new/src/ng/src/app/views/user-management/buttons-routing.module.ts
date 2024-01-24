import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { BarUnbarUserComponent } from './bar-unbar-user.component';
import { CreateUserComponent } from './create-user.component';
import { DeleteUserComponent } from './delete-user.component';
import { EditUserComponent } from './edit-user.component';
import { UserPermsComponent } from './user-perms.component';
import { UserReportComponent } from './user-report.component';

const routes: Routes = [
  {
    path: '',
    data: {
      title: 'user-management'
    },
    children: [
      {
        path: '',
        redirectTo: 'user-management'
      },
      {
        path: 'bar-unbar-user',
        component: BarUnbarUserComponent,
        data: {
          title: 'Bar UnBar User (Owner Account)'
        }
      },
      {
        path: 'create-user',
        component: CreateUserComponent,
        data: {
          title: 'Create User (Owner Account)'
        }
      },
      {
        path: 'delete-user',
        component: DeleteUserComponent,
        data: {
          title: 'delete-user'
        }
      },
      {
        path: 'edit-user',
        component: EditUserComponent,
        data: {
          title: 'edit-user'
        }
      }
      ,
      {
        path: 'user-perms',
        component: UserPermsComponent,
        data: {
          title: 'User Permissions'
        }
      },
      {
        path: 'user-report',
        component: UserReportComponent,
        data: {
          title: 'user-report'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ButtonsRoutingModule {}
