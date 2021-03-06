<!--
  - Copyright 2014-2018 the original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <section class="section" :class="{ 'is-loading' : !hasLoaded }">
    <div class="container" v-if="hasLoaded">
      <div v-if="error" class="message is-danger">
        <div class="message-body">
          <strong>
            <font-awesome-icon class="has-text-danger" icon="exclamation-triangle"/>
            Fetching scheduled tasks failed.
          </strong>
          <p v-text="error.message"/>
        </div>
      </div>
      <div v-else-if="!hasData" class="message is-warning">
        <div class="message-body">No scheduled tasks present.</div>
      </div>

      <template v-if="hasCronData">
        <h3 class="title">Cron</h3>
        <table class="table is-fullwidth">
          <thead>
            <tr>
              <th>Runnable</th>
              <th>Expression</th>
            </tr>
          </thead>
          <tbody v-for="task in cron" :key="task.runnable.target">
            <tr>
              <td v-text="task.runnable.target"/>
              <td class="monospaced" v-text="task.expression"/>
            </tr>
          </tbody>
        </table>
      </template>

      <template v-if="hasFixedDelayData">
        <h3 class="title">Fixed Delay</h3>
        <table class="metrics table is-fullwidth">
          <thead>
            <tr>
              <th>Runnable</th>
              <th>Initial Delay (ms)</th>
              <th>Interval (ms)</th>
            </tr>
          </thead>
          <tbody v-for="task in fixedDelay" :key="task.runnable.target">
            <tr>
              <td v-text="task.runnable.target"/>
              <td v-text="task.initialDelay"/>
              <td v-text="task.interval"/>
            </tr>
          </tbody>
        </table>
      </template>

      <template v-if="hasFixedRateData">
        <h3 class="title">Fixed Rate</h3>
        <table class="metrics table is-fullwidth">
          <thead>
            <tr>
              <th>Runnable</th>
              <th>Initial Delay (ms)</th>
              <th>Interval (ms)</th>
            </tr>
          </thead>
          <tbody v-for="task in fixedRate" :key="task.runnable.target">
            <tr>
              <td v-text="task.runnable.target"/>
              <td v-text="task.initialDelay"/>
              <td v-text="task.interval"/>
            </tr>
          </tbody>
        </table>
      </template>

    </div>
  </section>
</template>

<script>
  import Instance from '@/services/instance';

  export default {
    props: {
      instance: {
        type: Instance,
        required: true
      }
    },
    data: () => ({
      hasLoaded: false,
      error: null,
      cron: null,
      fixedDelay: null,
      fixedRate: null
    }),
    computed: {
      hasCronData: function () {
        return this.cron && this.cron.length;
      },
      hasFixedDelayData: function () {
        return this.fixedDelay && this.fixedDelay.length;
      },
      hasFixedRateData: function () {
        return this.fixedRate && this.fixedRate.length;
      },
      hasData: function () {
         return this.hasCronData || this.hasFixedDelayData || this.hasFixedRateData;
      }
    },
    created() {
      this.fetchScheduledTasks();
    },
    methods: {
      async fetchScheduledTasks() {
        this.error = null;
        try {
          const res = await this.instance.fetchScheduledTasks();
          this.cron = res.data.cron;
          this.fixedDelay = res.data.fixedDelay;
          this.fixedRate = res.data.fixedRate;
        } catch (error) {
          console.warn('Fetching scheduled tasks failed:', error);
          this.error = error;
        }
        this.hasLoaded = true;
      }
    },
    install({viewRegistry}) {
      viewRegistry.addView({
        name: 'instances/scheduledtasks',
        parent: 'instances',
        path: 'scheduledtasks',
        component: this,
        label: 'Scheduled Tasks',
        order: 950,
        isEnabled: ({instance}) => instance.hasEndpoint('scheduledtasks')
      });
    }
  }
</script>
