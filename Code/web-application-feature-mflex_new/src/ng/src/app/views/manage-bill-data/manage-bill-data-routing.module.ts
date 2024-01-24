import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { CreateBillDataComponent } from './create-bill-data.component';
import { EditDataComponent } from './edit-data.component';
import { ValidateDataComponent } from './validate-bill-data.component';

const routes: Routes = [
  {
    path: '',
    data: {
      title: 'Manage Bill Data'
    },
    children: [
      {
        path: '',
        redirectTo: 'manage-bill-data'
      },
      {
        path: 'create-bill-data',
        component: CreateBillDataComponent,
        data: {
          title: 'Create Bill Data'
        }
      },
      {
        path: 'edit-data',
        component: EditDataComponent,
        data: {
          title: 'Edit Data'
        }
      },
      {
        path: 'validate-bill-data',
        component: ValidateDataComponent,
        data: {
          title: 'validate-bill-data'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ManageBillDataRoutingModule {}
