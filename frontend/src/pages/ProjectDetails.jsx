import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { Plus, Users, ArrowLeft, MoreVertical, Calendar } from 'lucide-react';
import TaskForm from '../components/TaskForm';
import AddMemberModal from '../components/AddMemberModal';

const ProjectDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [project, setProject] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [showTaskForm, setShowTaskForm] = useState(false);
  const [showAddMember, setShowAddMember] = useState(false);
  const [editingTask, setEditingTask] = useState(null);

  useEffect(() => {
    fetchProjectData();
  }, [id]);

  const fetchProjectData = async () => {
    try {
      const [projRes, tasksRes, memRes] = await Promise.all([
        api.get(`/api/projects/${id}`),
        api.get(`/api/projects/${id}/tasks`),
        api.get(`/api/projects/${id}/members`)
      ]);
      setProject(projRes.data);
      setTasks(tasksRes.data);
      setMembers(memRes.data);
    } catch (error) {
      console.error('Failed to fetch project details', error);
      if (error.response?.status === 403) {
        navigate('/projects');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (taskId, newStatus) => {
    try {
      await api.patch(`/api/tasks/${taskId}/status`, { status: newStatus });
      fetchProjectData();
    } catch (error) {
      alert('Failed to update status. Only Admin or Assignee can do this.');
    }
  };

  const handleDeleteTask = async (taskId) => {
    if (!window.confirm('Are you sure you want to delete this task?')) return;
    try {
      await api.delete(`/api/tasks/${taskId}`);
      fetchProjectData();
    } catch (error) {
      alert('Failed to delete task. Only Admin can delete tasks.');
    }
  };

  const handleDeleteProject = async () => {
    if (!window.confirm('Are you sure you want to delete this entire project? This action cannot be undone.')) return;
    try {
      await api.delete(`/api/projects/${id}`);
      navigate('/projects');
    } catch (error) {
      alert('Failed to delete project. Only Admin can delete the project.');
    }
  };

  const handleRemoveMember = async (memberId) => {
    if (!window.confirm('Are you sure you want to remove this member?')) return;
    try {
      await api.delete(`/api/projects/${id}/members/${memberId}`);
      fetchProjectData();
    } catch (error) {
      alert(error.response?.data?.error || 'Failed to remove member.');
    }
  };

  if (loading) return <div className="flex justify-center py-10">Loading...</div>;
  if (!project) return <div>Project not found</div>;

  const isAdmin = user?.role === 'ADMIN';

  const statusColors = {
    TODO: 'bg-gray-100 text-gray-800',
    IN_PROGRESS: 'bg-blue-100 text-blue-800',
    DONE: 'bg-green-100 text-green-800'
  };

  const priorityColors = {
    LOW: 'text-green-600 bg-green-50',
    MEDIUM: 'text-yellow-600 bg-yellow-50',
    HIGH: 'text-red-600 bg-red-50'
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-4 mb-2">
        <button onClick={() => navigate('/projects')} className="text-gray-500 hover:text-gray-700">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h1 className="text-2xl font-bold text-gray-900">{project.name}</h1>
        <span className={`px-2 py-1 rounded text-xs font-medium ${isAdmin ? 'bg-purple-100 text-purple-800' : 'bg-green-100 text-green-800'}`}>
          {project.currentUserRole}
        </span>
      </div>
      <p className="text-gray-600 max-w-3xl">{project.description}</p>

      <div className="flex flex-col sm:flex-row justify-between gap-4 mt-8 mb-6 border-b border-gray-200 pb-4">
        <div className="flex space-x-2">
          {isAdmin && (
            <>
              <button
                onClick={() => { setEditingTask(null); setShowTaskForm(true); }}
                className="inline-flex items-center px-3 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700"
              >
                <Plus className="w-4 h-4 mr-2" /> Add Task
              </button>
              <button
                onClick={() => setShowAddMember(true)}
                className="inline-flex items-center px-3 py-2 border border-gray-300 text-sm font-medium rounded-md shadow-sm text-gray-700 bg-white hover:bg-gray-50"
              >
                <Users className="w-4 h-4 mr-2" /> Invite Member
              </button>
            </>
          )}
        </div>
        {isAdmin && (
          <button
            onClick={handleDeleteProject}
            className="text-red-600 hover:text-red-800 text-sm font-medium"
          >
            Delete Project
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Tasks</h2>
          <div className="space-y-4">
            {tasks.length === 0 ? (
              <p className="text-gray-500 text-sm italic">No tasks created yet.</p>
            ) : (
              tasks.map(task => (
                <div key={task.id} className="bg-white border border-gray-200 rounded-lg shadow-sm p-4 hover:shadow-md transition">
                  <div className="flex justify-between items-start">
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900">{task.title}</h3>
                      <p className="text-sm text-gray-500 mt-1">{task.description}</p>
                    </div>
                    <div className="flex space-x-2">
                      <select
                        value={task.status}
                        onChange={(e) => handleStatusChange(task.id, e.target.value)}
                        disabled={!isAdmin && (!task.assignee || task.assignee.id !== user.id)}
                        className={`text-xs font-semibold rounded-full px-2 py-1 border-0 ${statusColors[task.status]}`}
                      >
                        <option value="TODO">TODO</option>
                        <option value="IN_PROGRESS">IN PROGRESS</option>
                        <option value="DONE">DONE</option>
                      </select>
                      
                      {isAdmin && (
                        <div className="relative group inline-block">
                          <button className="text-gray-400 hover:text-gray-600 focus:outline-none">
                            <MoreVertical className="w-5 h-5" />
                          </button>
                          <div className="absolute right-0 mt-2 w-32 bg-white border border-gray-200 rounded-md shadow-lg hidden group-hover:block z-10">
                            <button
                              onClick={() => { setEditingTask(task); setShowTaskForm(true); }}
                              className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                            >
                              Edit
                            </button>
                            <button
                              onClick={() => handleDeleteTask(task.id)}
                              className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-100"
                            >
                              Delete
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                  
                  <div className="mt-4 flex items-center justify-between text-sm">
                    <div className="flex space-x-4">
                      <div className="flex items-center text-gray-500">
                        <Calendar className="w-4 h-4 mr-1" />
                        {task.dueDate ? new Date(task.dueDate).toLocaleDateString() : 'No date'}
                      </div>
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${priorityColors[task.priority]}`}>
                        {task.priority}
                      </span>
                    </div>
                    <div className="flex items-center">
                      <span className="text-gray-500 mr-2 text-xs">Assignee:</span>
                      {task.assignee ? (
                        <div className="flex items-center">
                          <div className="w-6 h-6 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center text-xs font-bold mr-1">
                            {task.assignee.name.charAt(0).toUpperCase()}
                          </div>
                          <span className="text-xs font-medium">{task.assignee.name}</span>
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400 italic">Unassigned</span>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div>
          <h2 className="text-lg font-medium text-gray-900 mb-4">Team Members</h2>
          <div className="bg-white border border-gray-200 rounded-lg shadow-sm">
            <ul className="divide-y divide-gray-200">
              {members.map(member => (
                <li key={member.id} className="p-4 flex items-center justify-between">
                  <div className="flex items-center">
                    <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-gray-600 font-bold text-sm">
                      {member.name.charAt(0).toUpperCase()}
                    </div>
                    <div className="ml-3">
                      <p className="text-sm font-medium text-gray-900">{member.name}</p>
                      <p className="text-xs text-gray-500">{member.email}</p>
                    </div>
                  </div>
                  {isAdmin && member.id !== user.id && (
                    <button
                      onClick={() => handleRemoveMember(member.id)}
                      className="text-xs text-red-600 hover:text-red-800"
                    >
                      Remove
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      {showTaskForm && (
        <TaskForm 
          projectId={id} 
          task={editingTask} 
          members={members}
          onClose={() => setShowTaskForm(false)} 
          onSuccess={() => { setShowTaskForm(false); fetchProjectData(); }}
        />
      )}

      {showAddMember && (
        <AddMemberModal 
          projectId={id} 
          onClose={() => setShowAddMember(false)} 
          onSuccess={() => { setShowAddMember(false); fetchProjectData(); }}
        />
      )}
    </div>
  );
};

export default ProjectDetails;
